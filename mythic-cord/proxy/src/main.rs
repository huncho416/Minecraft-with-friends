//! MythicCord proxy entry point.
//!
//! Two operating modes:
//!
//! * **Default build** (`cargo run -p mythiccord`):
//!   Standalone "registry citizen" — connects to mythic-stdb, announces
//!   itself, heartbeats, mirrors `server_registry`, and serves the admin
//!   HTTP surface. Does **not** accept Minecraft traffic. Useful for
//!   smoke-testing the STDB plumbing before the Infrarust subtree lands
//!   and for ops verification.
//!
//! * **Full build** (`cargo run -p mythiccord --features with-infrarust`):
//!   Wraps the vendored Infrarust binary, installs the routing plugin,
//!   accepts Minecraft handshakes on port 25565.
//!
//! Both modes share signal-handling, config loading, and the admin HTTP
//! surface so Pterodactyl manages them identically.

mod admin;
mod config;
mod state;

use anyhow::Context;
use config::Config;
use mythiccord_plugin_routing::{registry_view::RegistryView, ProxyIdentity, RoutingRuntime};
use mythiccord_stdb_bridge::{
    driver::{assert_schema_version, spawn_driver, DriverConfig},
    MythicStdbClient, ServerStatus,
};
use parking_lot::RwLock;
use state::ProxyState;
use std::path::PathBuf;
use std::sync::Arc;
use std::time::Duration;
use tracing::{info, warn};
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let cfg_path = std::env::args().nth(1).map(PathBuf::from);
    let cfg = Config::load(cfg_path.as_deref())?;
    init_tracing(&cfg);

    info!(
        version = env!("CARGO_PKG_VERSION"),
        shard = %cfg.identity.shard_id,
        role = %cfg.identity.role,
        "mythiccord starting",
    );

    let role = cfg.role()?;
    let identity = ProxyIdentity {
        shard_id: cfg.identity.shard_id.clone(),
        role,
        region: cfg.identity.region.clone(),
        address: cfg.identity.address.clone(),
        max_players: cfg.identity.max_players,
    };

    // ── STDB driver ──────────────────────────────────────────────────
    let driver_cfg = DriverConfig {
        stdb_uri: cfg.stdb.uri.clone(),
        module_name: cfg.stdb.module.clone(),
        ..Default::default()
    };
    let (handle, _driver_join) = spawn_driver(driver_cfg);

    // Schema version check — refuse to start on mismatch.
    if let Err(e) = assert_schema_version(&handle).await {
        anyhow::bail!("STDB schema check failed: {e}");
    }
    info!("schema version check ok");

    let stdb = Arc::new(MythicStdbClient::new(handle.clone()));

    // ── Registry mirror ──────────────────────────────────────────────
    let registry = RegistryView::new();
    mythiccord_plugin_routing::registry_view::spawn(handle.clone(), registry.clone());

    // ── Shared state ─────────────────────────────────────────────────
    let status = Arc::new(RwLock::new(ServerStatus::Starting));
    let state = Arc::new(ProxyState {
        identity: identity.clone(),
        stdb: stdb.clone(),
        registry: registry.clone(),
        status: status.clone(),
    });

    // ── Heartbeat task ───────────────────────────────────────────────
    let runtime = RoutingRuntime {
        identity: identity.clone(),
        stdb: stdb.clone(),
        registry: registry.clone(),
        status: status.clone(),
    };
    {
        let mut s = status.write();
        *s = ServerStatus::Healthy;
    }
    tokio::spawn(mythiccord_plugin_routing::heartbeat::run(runtime.clone()));

    // ── Admin HTTP ───────────────────────────────────────────────────
    let admin_bind: std::net::SocketAddr = cfg
        .admin
        .bind
        .parse()
        .with_context(|| format!("bad admin.bind: {}", cfg.admin.bind))?;
    let admin_state = state.clone();
    tokio::spawn(async move {
        if let Err(e) = admin::run(admin_bind, admin_state).await {
            warn!(?e, "admin HTTP exited");
        }
    });

    // ── Infrarust accept loop (optional feature) ────────────────────
    // When the feature is on, run Infrarust as a sibling task and race
    // its termination against the OS signal. When off, the signal IS the
    // primary loop — admin HTTP and heartbeat keep us busy in the meantime.
    #[cfg(feature = "with-infrarust")]
    let infrarust_task = {
        info!("starting Infrarust core with mythic-cord routing plugin");
        let runtime_for_infrarust = runtime.clone();
        Some(tokio::spawn(async move {
            // `infrarust::run_with_plugin` is the real entry once the
            // subtree is vendored; until then this branch doesn't compile.
            infrarust::run_with_plugin(runtime_for_infrarust).await
        }))
    };
    #[cfg(not(feature = "with-infrarust"))]
    let infrarust_task: Option<tokio::task::JoinHandle<anyhow::Result<()>>> = None;

    let shutdown_reason = tokio::select! {
        _ = wait_for_shutdown_signal() => "signal",
        result = wait_for_optional_task(infrarust_task) => {
            match result {
                Ok(Ok(())) => "infrarust exited cleanly",
                Ok(Err(e)) => { warn!(?e, "infrarust returned error"); "infrarust error" }
                Err(e) => { warn!(?e, "infrarust task panicked"); "infrarust panic" }
            }
        }
    };
    info!(reason = shutdown_reason, "shutdown — draining");

    {
        let mut s = status.write();
        *s = ServerStatus::Draining;
    }
    mythiccord_plugin_routing::heartbeat::announce_drain(&identity, &runtime).await;

    // Give STDB a beat to flush, then ack offline.
    tokio::time::sleep(Duration::from_millis(500)).await;
    {
        let mut s = status.write();
        *s = ServerStatus::Offline;
    }
    let _ = stdb
        .registry_heartbeat(&identity.shard_id, ServerStatus::Offline, 0, 0.0, 0.0)
        .await;

    handle.shutdown().await;
    info!("clean exit");
    Ok(())
}

fn init_tracing(cfg: &Config) {
    let filter = EnvFilter::try_new(&cfg.log.filter)
        .unwrap_or_else(|_| EnvFilter::new("info"));
    let registry = tracing_subscriber::fmt().with_env_filter(filter);
    if cfg.log.format == "json" {
        registry.json().with_current_span(false).init();
    } else {
        registry.init();
    }
}

/// Await an optional task. When `None`, awaits forever — used in the
/// `tokio::select!` so the signal branch is the only thing that fires
/// in standalone mode.
async fn wait_for_optional_task(
    task: Option<tokio::task::JoinHandle<anyhow::Result<()>>>,
) -> Result<anyhow::Result<()>, tokio::task::JoinError> {
    match task {
        Some(handle) => handle.await,
        None => std::future::pending().await,
    }
}

#[cfg(unix)]
async fn wait_for_shutdown_signal() {
    use tokio::signal::unix::{signal, SignalKind};
    let mut term = signal(SignalKind::terminate()).expect("install SIGTERM handler");
    let mut intr = signal(SignalKind::interrupt()).expect("install SIGINT handler");
    tokio::select! {
        _ = term.recv() => info!("got SIGTERM"),
        _ = intr.recv() => info!("got SIGINT"),
    }
}

#[cfg(not(unix))]
async fn wait_for_shutdown_signal() {
    let _ = tokio::signal::ctrl_c().await;
    info!("got Ctrl-C");
}

#![allow(clippy::doc_markdown)]

mod admin;
mod config;
#[cfg(feature = "with-infrarust")]
mod config_export;
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

    let driver_cfg = DriverConfig {
        stdb_uri: cfg.stdb.uri.clone(),
        module_name: cfg.stdb.module.clone(),
        ..Default::default()
    };
    let (handle, _driver_join) = spawn_driver(driver_cfg);

    if let Err(e) = assert_schema_version(&handle).await {
        anyhow::bail!("STDB schema check failed: {e}");
    }
    info!("schema version check ok");

    let stdb = Arc::new(MythicStdbClient::new(handle.clone()));

    let registry = RegistryView::new();
    mythiccord_plugin_routing::registry_view::spawn(handle.clone(), registry.clone());
    mythiccord_plugin_routing::registry_view::spawn_http_poll(cfg.stdb.uri.clone(), registry.clone());

    let status = Arc::new(RwLock::new(ServerStatus::Starting));
    let state = Arc::new(ProxyState {
        identity: identity.clone(),
        stdb: stdb.clone(),
        registry: registry.clone(),
        status: status.clone(),
    });

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

    #[cfg(feature = "with-infrarust")]
    {
        let exporter = config_export::ConfigExporter::from_config(registry.clone(), &cfg);
        tokio::spawn(exporter.run());
        info!("config exporter started; Infrarust runs as a separate sidecar process");
    }

    wait_for_shutdown_signal().await;
    info!("shutdown signal received — draining");

    {
        let mut s = status.write();
        *s = ServerStatus::Draining;
    }
    mythiccord_plugin_routing::heartbeat::announce_drain(&identity, &runtime).await;

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

//! Periodic heartbeat task. Calls `registry_announce` once at startup
//! then `registry_heartbeat` on a 5s interval. Always reports the
//! current proxy status so a SIGTERM-driven flip to `Draining` propagates
//! within one tick.

use crate::{ProxyIdentity, RoutingRuntime};
use mythiccord_stdb_bridge::ServerStatus;
use std::time::Duration;
use tracing::{info, warn};

/// Tunable: how often to send a heartbeat. STDB's stale-detector should
/// be configured to ~3× this interval before flipping a shard offline.
pub const HEARTBEAT_INTERVAL: Duration = Duration::from_secs(5);

/// Run forever (until the runtime is dropped). Spawn this with
/// `tokio::spawn(heartbeat::run(runtime))`.
pub async fn run(runtime: RoutingRuntime) {
    let RoutingRuntime { identity, stdb, .. } = &runtime;

    if let Err(e) = stdb
        .registry_announce(
            &identity.shard_id,
            identity.role,
            &identity.region,
            &identity.address,
            identity.max_players,
            mythiccord_stdb_bridge::schema::SCHEMA_VERSION,
        )
        .await
    {
        warn!("registry_announce failed at boot: {e}");
    } else {
        info!(
            shard = %identity.shard_id, role = ?identity.role,
            "announced to mythic-stdb"
        );
    }

    let mut tick = tokio::time::interval(HEARTBEAT_INTERVAL);
    tick.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Delay);
    loop {
        tick.tick().await;
        let snapshot = current_status(&runtime);
        let player_count = current_player_count(&runtime);
        let tps = 20.0; // proxy doesn't have TPS — game servers report theirs
        let heap = current_heap_load();
        if let Err(e) = stdb
            .registry_heartbeat(
                &identity.shard_id,
                snapshot,
                player_count,
                tps,
                heap,
            )
            .await
        {
            warn!("registry_heartbeat failed: {e}");
        }
        if matches!(snapshot, ServerStatus::Offline) {
            info!("status=OFFLINE — heartbeat task exiting");
            return;
        }
    }
}

/// Drain step. Tells STDB no new connections, so other proxies stop
/// routing here. Idempotent.
pub async fn announce_drain(identity: &ProxyIdentity, runtime: &RoutingRuntime) {
    if let Err(e) = runtime.stdb.registry_drain(&identity.shard_id).await {
        warn!("registry_drain failed: {e}");
    } else {
        info!(shard = %identity.shard_id, "registered drain with mythic-stdb");
    }
}

fn current_status(runtime: &RoutingRuntime) -> ServerStatus {
    *runtime.status.read()
}

fn current_player_count(_runtime: &RoutingRuntime) -> u32 {
    // Wired up once `integration::session_state` exists in the with-infrarust
    // build. Returning 0 is a safe placeholder — the registry row will reflect
    // this proxy as empty until then.
    0
}

fn current_heap_load() -> f32 {
    // Rust doesn't have a portable heap-load metric the way the JVM does;
    // we report 0.0 and let Prometheus track process_resident_memory_bytes
    // for the host-side view.
    0.0
}

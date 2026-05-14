

use crate::{ProxyIdentity, RoutingRuntime};
use mythiccord_stdb_bridge::ServerStatus;
use std::time::Duration;
use tracing::{info, warn};

pub const HEARTBEAT_INTERVAL: Duration = Duration::from_secs(5);

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
        let tps = 20.0;
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

    0
}

fn current_heap_load() -> f32 {

    0.0
}

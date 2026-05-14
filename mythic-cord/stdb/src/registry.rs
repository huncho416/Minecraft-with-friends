

use crate::common::{require_backend, server_status, ShardId};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = server_registry, public)]
pub struct ServerEntry {

    #[primary_key]
    pub shard_id: ShardId,

    #[index(btree)]
    pub role: String,

    #[index(btree)]
    pub region: String,

    #[index(btree)]
    pub status: String,

    pub address: String,

    pub max_players: u32,

    pub player_count: u32,

    pub tps: f32,

    pub heap_load: f32,

    pub schema_version: u32,

    pub started_at: Timestamp,
    pub last_heartbeat: Timestamp,
}

pub fn load_score(e: &ServerEntry) -> f32 {
    let cap = e.max_players.max(1) as f32;
    let saturation = (e.player_count as f32) / cap;
    let tps_penalty = (20.0 - e.tps.min(20.0)) / 20.0;
    let heap_penalty = e.heap_load.clamp(0.0, 1.0);
    saturation + 0.5 * tps_penalty + 0.3 * heap_penalty
}

#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn registry_announce(
    ctx: &ReducerContext,
    shard_id: ShardId,
    role: String,
    region: String,
    address: String,
    max_players: u32,
    schema_version: u32,
) -> Result<(), String> {
    require_backend(ctx)?;
    if shard_id.is_empty() {
        reject!("shard_id must not be empty");
    }
    let reg = ctx.db.server_registry();
    if let Some(mut e) = reg.shard_id().find(shard_id.clone()) {
        e.role = role;
        e.region = region;
        e.address = address;
        e.max_players = max_players;
        e.schema_version = schema_version;
        e.status = server_status::STARTING.to_string();
        e.last_heartbeat = ctx.timestamp;
        reg.shard_id().update(e);
    } else {
        reg.insert(ServerEntry {
            shard_id,
            role,
            region,
            status: server_status::STARTING.to_string(),
            address,
            max_players,
            player_count: 0,
            tps: 20.0,
            heap_load: 0.0,
            schema_version,
            started_at: ctx.timestamp,
            last_heartbeat: ctx.timestamp,
        });
    }
    Ok(())
}

#[reducer]
pub fn registry_heartbeat(
    ctx: &ReducerContext,
    shard_id: ShardId,
    status: String,
    player_count: u32,
    tps: f32,
    heap_load: f32,
) -> Result<(), String> {
    require_backend(ctx)?;
    if !matches!(
        status.as_str(),
        server_status::STARTING
            | server_status::HEALTHY
            | server_status::DEGRADED
            | server_status::DRAINING
            | server_status::OFFLINE
    ) {
        reject!("unknown status: {status}");
    }
    let reg = ctx.db.server_registry();
    let Some(mut e) = reg.shard_id().find(shard_id.clone()) else {
        reject!("unknown shard: {shard_id} (call registry_announce first)");
    };
    e.status = status;
    e.player_count = player_count;
    e.tps = tps;
    e.heap_load = heap_load;
    e.last_heartbeat = ctx.timestamp;
    reg.shard_id().update(e);
    Ok(())
}

#[reducer]
pub fn registry_drain(ctx: &ReducerContext, shard_id: ShardId) -> Result<(), String> {
    require_backend(ctx)?;
    let reg = ctx.db.server_registry();
    let Some(mut e) = reg.shard_id().find(shard_id.clone()) else {
        reject!("unknown shard: {shard_id}");
    };
    e.status = server_status::DRAINING.to_string();
    e.last_heartbeat = ctx.timestamp;
    reg.shard_id().update(e);
    Ok(())
}

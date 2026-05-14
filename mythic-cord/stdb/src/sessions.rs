

use crate::common::{require_backend, require_uuid, PlayerUuid, ReducerResult, ShardId};

use crate::players::{players, upsert_player};
use crate::reject;
use crate::registry::{load_score, ServerEntry};
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = sessions, public)]
pub struct Session {

    #[primary_key]
    pub player_uuid: PlayerUuid,

    #[index(btree)]
    pub username: String,

    #[index(btree)]
    pub shard_id: ShardId,

    pub proxy_session_id: u64,

    pub ip_hash: String,

    pub region: String,

    pub vanished: bool,

    pub login_at: Timestamp,
    pub last_activity: Timestamp,
}

#[table(name = session_history, public)]
pub struct SessionEvent {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    #[index(btree)]
    pub shard_id: ShardId,

    pub event_type: String,

    pub reason: String,

    pub at: Timestamp,
}

pub(crate) fn on_client_connected(_ctx: &ReducerContext) {

}

pub(crate) fn on_client_disconnected(ctx: &ReducerContext) {

    let _ = ctx;
}

#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn session_login(
    ctx: &ReducerContext,
    uuid: PlayerUuid,
    username: String,
    shard_id: ShardId,
    proxy_session_id: u64,
    ip_hash: String,
    region: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    upsert_player(ctx, &uuid, &username)?;

    let sessions = ctx.db.sessions();
    let row = Session {
        player_uuid: uuid.clone(),
        username: username.clone(),
        shard_id: shard_id.clone(),
        proxy_session_id,
        ip_hash,
        region: region.clone(),
        vanished: false,
        login_at: ctx.timestamp,
        last_activity: ctx.timestamp,
    };
    if sessions.player_uuid().find(uuid.clone()).is_some() {
        sessions.player_uuid().update(row);
    } else {
        sessions.insert(row);
    }

    let players = ctx.db.players();
    if let Some(mut p) = players.uuid().find(uuid.clone()) {
        p.online = true;
        p.current_shard = shard_id.clone();
        p.region = region;
        p.last_seen = ctx.timestamp;
        players.uuid().update(p);
    }

    ctx.db.session_history().insert(SessionEvent {
        id: 0,
        player_uuid: uuid,
        shard_id,
        event_type: "LOGIN".to_string(),
        reason: String::new(),
        at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn session_logout(ctx: &ReducerContext, uuid: PlayerUuid, reason: String) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    let sessions = ctx.db.sessions();
    let Some(s) = sessions.player_uuid().find(uuid.clone()) else {
        return Ok(());
    };
    let shard_id = s.shard_id.clone();

    let elapsed_micros = (ctx.timestamp.to_micros_since_unix_epoch()
        - s.login_at.to_micros_since_unix_epoch())
        .max(0);
    let session_seconds = u64::try_from(elapsed_micros).unwrap_or(0) / 1_000_000;
    sessions.player_uuid().delete(uuid.clone());

    let players = ctx.db.players();
    if let Some(mut p) = players.uuid().find(uuid.clone()) {
        p.online = false;
        p.current_shard = String::new();
        p.last_seen = ctx.timestamp;
        p.playtime_seconds = p.playtime_seconds.saturating_add(session_seconds);
        players.uuid().update(p);
    }

    ctx.db.session_history().insert(SessionEvent {
        id: 0,
        player_uuid: uuid,
        shard_id,
        event_type: "LOGOUT".to_string(),
        reason,
        at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn session_route(
    ctx: &ReducerContext,
    uuid: PlayerUuid,
    new_shard_id: ShardId,
    reason: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    let sessions = ctx.db.sessions();
    let Some(mut s) = sessions.player_uuid().find(uuid.clone()) else {
        reject!("no active session for {uuid}");
    };
    let old = std::mem::replace(&mut s.shard_id, new_shard_id.clone());
    s.last_activity = ctx.timestamp;
    sessions.player_uuid().update(s);

    if let Some(mut p) = ctx.db.players().uuid().find(uuid.clone()) {
        p.current_shard = new_shard_id.clone();
        p.last_seen = ctx.timestamp;
        ctx.db.players().uuid().update(p);
    }

    ctx.db.session_history().insert(SessionEvent {
        id: 0,
        player_uuid: uuid,
        shard_id: new_shard_id,
        event_type: "ROUTE".to_string(),
        reason: format!("from={old} {reason}"),
        at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn session_touch(ctx: &ReducerContext, uuid: PlayerUuid) -> ReducerResult {
    require_backend(ctx)?;
    let sessions = ctx.db.sessions();
    if let Some(mut s) = sessions.player_uuid().find(uuid) {
        s.last_activity = ctx.timestamp;
        sessions.player_uuid().update(s);
    }
    Ok(())
}

#[reducer]
pub fn session_reap(ctx: &ReducerContext, older_than_seconds: u64) -> ReducerResult {
    require_backend(ctx)?;

    let older_micros = i64::try_from(older_than_seconds)
        .unwrap_or(i64::MAX)
        .saturating_mul(1_000_000);
    let cutoff_micros = ctx
        .timestamp
        .to_micros_since_unix_epoch()
        .saturating_sub(older_micros);
    let sessions = ctx.db.sessions();
    let stale: Vec<PlayerUuid> = sessions
        .iter()
        .filter(|s| s.last_activity.to_micros_since_unix_epoch() < cutoff_micros)
        .map(|s| s.player_uuid.clone())
        .collect();
    for uuid in &stale {
        let _ = session_logout(ctx, uuid.clone(), "reaped".to_string());
    }
    log::info!("session_reap removed {} stale rows", stale.len());
    Ok(())
}

pub fn pick_shard<'a>(
    candidates: impl Iterator<Item = &'a ServerEntry>,
    desired_role: &str,
    preferred_region: &str,
) -> Option<&'a ServerEntry> {
    candidates
        .filter(|e| e.role == desired_role)
        .filter(|e| e.status == crate::common::server_status::HEALTHY)
        .filter(|e| e.player_count < e.max_players)
        .min_by(|a, b| {

            let region_a = u32::from(a.region != preferred_region);
            let region_b = u32::from(b.region != preferred_region);
            load_score(a)
                .partial_cmp(&load_score(b))
                .unwrap_or(std::cmp::Ordering::Equal)
                .then(region_a.cmp(&region_b))
                .then(a.shard_id.cmp(&b.shard_id))
        })
}

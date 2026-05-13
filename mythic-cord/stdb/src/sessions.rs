//! Sessions, routing, and connection lifecycle.
//!
//! - [`sessions`] tracks who is connected, where, and from which IP.
//! - [`session_history`] is an append-only audit log (login/logout/server
//!   switch events) used for analytics and abuse investigation.
//! - The proxy calls [`session_login`] on handshake-complete, and
//!   [`session_route`] when transferring a player between shards.

use crate::common::{require_backend, require_uuid, PlayerUuid, ReducerResult, ShardId};
// Cross-module table accessors: SpacetimeDB's `#[table]` macro generates a
// trait named after the table (lowercase). Calling `ctx.db.<table>()` from
// another module requires that trait in scope.
use crate::players::{players, upsert_player};
use crate::reject;
use crate::registry::{load_score, ServerEntry};
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = sessions, public)]
pub struct Session {
    /// One row per online player.
    #[primary_key]
    pub player_uuid: PlayerUuid,

    /// Last-known username (denormalized for cheap subscriptions).
    #[index(btree)]
    pub username: String,

    /// Shard the player is connected to.
    #[index(btree)]
    pub shard_id: ShardId,

    /// Stable id of the proxy connection. Useful for kicking by session
    /// without racing against reconnects.
    pub proxy_session_id: u64,

    /// Hashed IP — never store raw IPs in the public table.
    pub ip_hash: String,

    /// Geo region the proxy assigned at login.
    pub region: String,

    /// `false` for normal players, `true` for staff vanish/disguise sessions.
    pub vanished: bool,

    pub login_at: Timestamp,
    pub last_activity: Timestamp,
}

/// Append-only history for analytics. Truncated periodically by ops.
#[table(name = session_history, public)]
pub struct SessionEvent {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    #[index(btree)]
    pub shard_id: ShardId,

    /// `LOGIN`, `LOGOUT`, `ROUTE`, `KICK`.
    pub event_type: String,

    /// Optional reason (kick reason, route trigger, etc.).
    pub reason: String,

    pub at: Timestamp,
}

// ── Connection-lifecycle hooks (called from lib.rs) ──────────────────

pub(crate) fn on_client_connected(_ctx: &ReducerContext) {
    // We don't have player UUID at this layer (clients connect anonymously
    // to STDB). The proxy calls `session_login` once it knows the UUID.
}

pub(crate) fn on_client_disconnected(ctx: &ReducerContext) {
    // The proxy is the authoritative source for player disconnects; client
    // STDB sockets dropping doesn't always mean the player left. We rely
    // on `session_logout` from the proxy. Best-effort cleanup of stale
    // rows happens in `session_reap` below.
    let _ = ctx;
}

// ── Reducers ──────────────────────────────────────────────────────────

/// Called by the proxy when a player completes login. Idempotent: if a
/// session row exists (e.g. quick reconnect), it's overwritten.
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

    // Sync the players row.
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

/// Called by the proxy when a player disconnects.
#[reducer]
pub fn session_logout(ctx: &ReducerContext, uuid: PlayerUuid, reason: String) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    let sessions = ctx.db.sessions();
    let Some(s) = sessions.player_uuid().find(uuid.clone()) else {
        return Ok(()); // already gone
    };
    let shard_id = s.shard_id.clone();
    // `.max(0)` guarantees non-negative; the `as u64` cast is then exact.
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

/// Called when the proxy moves a player between shards (e.g. hub → skyblock).
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

/// Touch `last_activity` — proxy calls this on chat / movement keepalive
/// so we can detect zombie sessions.
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

/// Periodic janitor: drop sessions whose proxy heartbeat hasn't touched
/// them in `older_than_seconds`. Called from a cron in the proxy.
#[reducer]
pub fn session_reap(ctx: &ReducerContext, older_than_seconds: u64) -> ReducerResult {
    require_backend(ctx)?;
    // u64 → i64 saturating: ages > i64::MAX seconds (≈292B years) clamp to 0.
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

// ── Helpers exposed for the proxy ────────────────────────────────────

/// Pure helper: pick the best target shard for a player given role and
/// region preference. Lower [`load_score`] wins; ties broken by region
/// match, then by shard id for stability.
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
            // 0 when region matches, 1 otherwise — sorts matching regions first.
            let region_a = u32::from(a.region != preferred_region);
            let region_b = u32::from(b.region != preferred_region);
            load_score(a)
                .partial_cmp(&load_score(b))
                .unwrap_or(std::cmp::Ordering::Equal)
                .then(region_a.cmp(&region_b))
                .then(a.shard_id.cmp(&b.shard_id))
        })
}

//! Player identity, rank, currencies (mirror), and presence summary.
//!
//! This is the canonical player row. Other tables reference it by
//! `uuid` (Mojang UUID, hyphenated). The economy module owns the
//! transaction log; balances are also denormalized here for fast reads
//! during login and tab-list rendering.

use crate::common::{currency, require_backend, require_uuid, PlayerUuid, ReducerResult, ShardId};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = players, public)]
pub struct Player {
    /// Mojang UUID, 36-char hyphenated.
    #[primary_key]
    pub uuid: PlayerUuid,

    /// Last-seen username. Mojang allows renames — `username_history`
    /// (future) will track changes.
    #[index(btree)]
    pub username: String,

    /// Lowercased username for case-insensitive lookups.
    #[index(btree)]
    pub username_lower: String,

    /// Rank key (matches `suite-permission` Rank ids). Empty = default.
    pub rank: String,

    /// Denormalized balances. Source of truth is the transaction log in
    /// [`crate::economy`] — these are updated by economy reducers atomically.
    pub coins: i64,
    pub points: i64,
    pub gems: i64,

    /// Which shard the player is currently on. Empty when offline.
    pub current_shard: ShardId,

    /// Geographic region tag (e.g. `us-east`, `eu-west`).
    pub region: String,

    /// Online flag derived from `sessions` — denormalized so client
    /// subscriptions can filter cheaply without joining.
    pub online: bool,

    /// First-time login.
    pub first_join: Timestamp,
    /// Most recent activity.
    pub last_seen: Timestamp,

    /// Total play-time in seconds (accumulated server-side on disconnect).
    pub playtime_seconds: u64,
}

impl Player {
    fn new(uuid: PlayerUuid, username: String, now: Timestamp) -> Self {
        let username_lower = username.to_lowercase();
        Self {
            uuid,
            username,
            username_lower,
            rank: String::new(),
            coins: 0,
            points: 0,
            gems: 0,
            current_shard: String::new(),
            region: String::new(),
            online: false,
            first_join: now,
            last_seen: now,
            playtime_seconds: 0,
        }
    }
}

/// Create-if-missing helper used by `player_login`. Returns whether the
/// row was newly inserted (true) or already existed (false).
pub fn upsert_player(
    ctx: &ReducerContext,
    uuid: &str,
    username: &str,
) -> ReducerResult<bool> {
    require_uuid(uuid)?;
    let players = ctx.db.players();
    if let Some(mut p) = players.uuid().find(uuid.to_string()) {
        if p.username != username {
            p.username = username.to_string();
            p.username_lower = username.to_lowercase();
        }
        p.last_seen = ctx.timestamp;
        players.uuid().update(p);
        Ok(false)
    } else {
        players.insert(Player::new(uuid.to_string(), username.to_string(), ctx.timestamp));
        Ok(true)
    }
}

/// Update the player's denormalized balance for a currency. Called from
/// the economy module after appending the transaction log row.
pub(crate) fn adjust_balance(
    ctx: &ReducerContext,
    uuid: &str,
    currency_code: &str,
    delta: i64,
) -> ReducerResult<i64> {
    let players = ctx.db.players();
    let Some(mut p) = players.uuid().find(uuid.to_string()) else {
        reject!("player not found: {uuid}");
    };
    let new_balance = match currency_code {
        currency::COINS => {
            p.coins = p.coins.saturating_add(delta);
            p.coins
        }
        currency::POINTS => {
            p.points = p.points.saturating_add(delta);
            p.points
        }
        currency::GEMS => {
            p.gems = p.gems.saturating_add(delta);
            p.gems
        }
        other => reject!("unknown currency: {other}"),
    };
    if new_balance < 0 {
        reject!("insufficient {currency_code} for {uuid}");
    }
    players.uuid().update(p);
    Ok(new_balance)
}

// ── Reducers ──────────────────────────────────────────────────────────

/// Set/promote a player's rank. Called by staff commands.
#[reducer]
pub fn player_set_rank(ctx: &ReducerContext, uuid: String, rank: String) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    let players = ctx.db.players();
    let Some(mut p) = players.uuid().find(uuid.clone()) else {
        reject!("player not found: {uuid}");
    };
    p.rank = rank;
    p.last_seen = ctx.timestamp;
    players.uuid().update(p);
    Ok(())
}

/// Update the player's region tag — typically called once on first login
/// after the proxy geo-resolves their IP.
#[reducer]
pub fn player_set_region(ctx: &ReducerContext, uuid: String, region: String) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    let players = ctx.db.players();
    let Some(mut p) = players.uuid().find(uuid.clone()) else {
        reject!("player not found: {uuid}");
    };
    p.region = region;
    players.uuid().update(p);
    Ok(())
}



use crate::common::{currency, require_backend, require_uuid, PlayerUuid, ReducerResult, ShardId};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = players, public)]
pub struct Player {

    #[primary_key]
    pub uuid: PlayerUuid,

    #[index(btree)]
    pub username: String,

    #[index(btree)]
    pub username_lower: String,

    pub rank: String,

    pub coins: i64,
    pub points: i64,
    pub gems: i64,

    pub current_shard: ShardId,

    pub region: String,

    pub online: bool,

    pub first_join: Timestamp,

    pub last_seen: Timestamp,

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



use crate::common::{require_backend, require_uuid, PlayerUuid, ReducerResult, ShardId};

use crate::players::players;
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = islands, public)]
pub struct Island {
    #[primary_key]
    pub island_id: String,

    #[index(btree)]
    pub owner_uuid: PlayerUuid,

    #[index(btree)]
    pub shard_id: ShardId,

    pub level: u32,

    pub total_points: i64,

    pub size_tier: String,

    pub created_at: Timestamp,
    pub last_visited: Timestamp,
}

#[table(name = island_members, public)]
pub struct IslandMember {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub island_id: String,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    pub role: String,

    pub joined_at: Timestamp,
}

#[reducer]
pub fn island_create(
    ctx: &ReducerContext,
    island_id: String,
    owner_uuid: PlayerUuid,
    shard_id: ShardId,
    size_tier: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&owner_uuid)?;
    if ctx.db.islands().island_id().find(island_id.clone()).is_some() {
        reject!("island {island_id} already exists");
    }
    ctx.db.islands().insert(Island {
        island_id: island_id.clone(),
        owner_uuid: owner_uuid.clone(),
        shard_id,
        level: 1,
        total_points: 0,
        size_tier,
        created_at: ctx.timestamp,
        last_visited: ctx.timestamp,
    });
    ctx.db.island_members().insert(IslandMember {
        id: 0,
        island_id,
        player_uuid: owner_uuid,
        role: "OWNER".to_string(),
        joined_at: ctx.timestamp,
    });
    Ok(())
}

#[table(name = skills, public)]
pub struct SkillRow {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    #[index(btree)]
    pub skill: String,

    pub xp: u64,
    pub level: u32,
    pub last_updated: Timestamp,
}

#[reducer]
pub fn skill_grant_xp(
    ctx: &ReducerContext,
    player_uuid: PlayerUuid,
    skill: String,
    xp_delta: u64,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&player_uuid)?;
    if xp_delta == 0 {
        return Ok(());
    }
    let skills = ctx.db.skills();
    let existing: Option<SkillRow> = skills
        .iter()
        .find(|s| s.player_uuid == player_uuid && s.skill == skill);
    if let Some(mut s) = existing {
        s.xp = s.xp.saturating_add(xp_delta);
        s.level = level_for_xp(s.xp);
        s.last_updated = ctx.timestamp;
        skills.id().update(s);
    } else {
        let xp = xp_delta;
        skills.insert(SkillRow {
            id: 0,
            player_uuid,
            skill,
            xp,
            level: level_for_xp(xp),
            last_updated: ctx.timestamp,
        });
    }
    Ok(())
}

fn level_for_xp(xp: u64) -> u32 {
    ((xp as f64 / 100.0).sqrt().floor() as u32).max(1)
}

#[table(name = stats, public)]
pub struct StatRow {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    #[index(btree)]
    pub stat: String,

    pub value_daily: i64,

    pub value_weekly: i64,

    pub value_alltime: i64,

    pub last_updated: Timestamp,
}

#[reducer]
pub fn stat_increment(
    ctx: &ReducerContext,
    player_uuid: PlayerUuid,
    stat: String,
    delta: i64,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&player_uuid)?;
    if delta == 0 {
        return Ok(());
    }
    let stats = ctx.db.stats();
    let existing: Option<StatRow> = stats
        .iter()
        .find(|s| s.player_uuid == player_uuid && s.stat == stat);
    if let Some(mut s) = existing {
        s.value_daily = s.value_daily.saturating_add(delta);
        s.value_weekly = s.value_weekly.saturating_add(delta);
        s.value_alltime = s.value_alltime.saturating_add(delta);
        s.last_updated = ctx.timestamp;
        stats.id().update(s);
    } else {
        stats.insert(StatRow {
            id: 0,
            player_uuid,
            stat,
            value_daily: delta,
            value_weekly: delta,
            value_alltime: delta,
            last_updated: ctx.timestamp,
        });
    }
    Ok(())
}

#[reducer]
pub fn stats_reset_daily(ctx: &ReducerContext) -> ReducerResult {
    require_backend(ctx)?;
    let stats = ctx.db.stats();
    let touched: Vec<StatRow> = stats.iter().filter(|s| s.value_daily != 0).collect();
    for mut s in touched {
        s.value_daily = 0;
        stats.id().update(s);
    }
    Ok(())
}

#[reducer]
pub fn stats_reset_weekly(ctx: &ReducerContext) -> ReducerResult {
    require_backend(ctx)?;
    let stats = ctx.db.stats();
    let touched: Vec<StatRow> = stats.iter().filter(|s| s.value_weekly != 0).collect();
    for mut s in touched {
        s.value_weekly = 0;
        stats.id().update(s);
    }
    Ok(())
}

#[table(name = leaderboards, public)]
pub struct LeaderboardRow {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub board: String,

    #[index(btree)]
    pub timeframe: String,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    pub username: String,
    pub score: i64,

    pub rank: u32,

    pub computed_at: Timestamp,
}

#[reducer]
pub fn leaderboard_rebuild(
    ctx: &ReducerContext,
    board: String,
    timeframe: String,
    stat_key: String,
    top_n: u32,
) -> ReducerResult {
    require_backend(ctx)?;
    if !matches!(timeframe.as_str(), "DAILY" | "WEEKLY" | "MONTHLY" | "ALLTIME") {
        reject!("invalid timeframe: {timeframe}");
    }

    let mut rows: Vec<(PlayerUuid, i64)> = ctx
        .db
        .stats()
        .iter()
        .filter(|s| s.stat == stat_key)
        .map(|s| {
            let score = match timeframe.as_str() {
                "DAILY" => s.value_daily,
                "WEEKLY" => s.value_weekly,
                "MONTHLY" => s.value_weekly,
                _ => s.value_alltime,
            };
            (s.player_uuid, score)
        })
        .collect();

    rows.sort_unstable_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(&b.0)));
    rows.truncate(top_n as usize);

    let lb = ctx.db.leaderboards();
    let prior: Vec<u64> = lb
        .iter()
        .filter(|r| r.board == board && r.timeframe == timeframe)
        .map(|r| r.id)
        .collect();
    for id in prior {
        lb.id().delete(id);
    }

    let players = ctx.db.players();
    for (i, (uuid, score)) in rows.into_iter().enumerate() {
        let username = players
            .uuid()
            .find(uuid.clone())
            .map(|p| p.username)
            .unwrap_or_default();
        lb.insert(LeaderboardRow {
            id: 0,
            board: board.clone(),
            timeframe: timeframe.clone(),
            player_uuid: uuid,
            username,
            score,
            rank: (i as u32) + 1,
            computed_at: ctx.timestamp,
        });
    }
    Ok(())
}

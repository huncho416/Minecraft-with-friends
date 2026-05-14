//! Ranks: definitions + grants.
//!
//! Mirrors mythic-core's `CoreRank` and `RankGrant` records. Two tables:
//! - `rank_definitions` — one row per rank id (default, vip, mod, owner…).
//!   Edits in-place via `rank_define`. YAML-seeded at boot.
//! - `rank_grants` — append-only history of every grant ever issued.
//!   Phase 3 keeps inactive grants visible until explicitly removed
//!   (PLAN line 683), so we soft-delete via `active=false`.

use crate::common::{require_backend, require_uuid, PlayerUuid, ReducerResult};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

// ── Rank definitions ─────────────────────────────────────────────────

#[table(name = rank_definitions, public)]
pub struct RankDefinition {
    /// Lowercased rank id (e.g. `default`, `vip`, `owner`).
    #[primary_key]
    pub id: String,

    /// Display name (preserved casing).
    pub display_name: String,

    /// Hex color for the rank (`#RRGGBB`).
    pub color: String,

    /// Bukkit `Material` name for the rank-selection menu icon
    /// (e.g. `WHITE_DYE`). Stored as a string so the schema doesn't
    /// pin a Bukkit version.
    pub dye: String,

    /// Generic prefix (legacy / fallback when display-specific is empty).
    pub prefix: String,

    /// Generic suffix.
    pub suffix: String,

    /// Lower wins when a player has multiple ranks (1 = highest priority).
    #[index(btree)]
    pub weight: i32,

    /// Staff rank flag.
    pub staff: bool,

    /// Donator/purchasable flag.
    pub donator: bool,

    /// Parent rank id for inheritance display (empty = no parent).
    pub parent: String,

    /// JSON-encoded array of permission strings. Stored as JSON because
    /// SpacetimeDB doesn't natively support `Vec<String>` columns.
    pub permissions_json: String,

    /// Display-specific fields. All support hex strings via the suite's
    /// MythicHex parser. Empty string = fall back to `prefix`/`suffix`.
    pub chat_prefix: String,
    pub chat_format: String,
    pub tab_prefix: String,
    pub tab_format: String,
    pub nametag_prefix: String,
    pub nametag_format: String,

    /// `true` for ranks seeded from YAML at boot. Lets ops distinguish
    /// hand-edited definitions from defaults that may be re-seeded.
    pub seeded: bool,

    pub created_at: Timestamp,
    pub updated_at: Timestamp,
}

// ── Rank grants ──────────────────────────────────────────────────────

#[table(name = rank_grants, public)]
pub struct RankGrant {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub target_uuid: PlayerUuid,

    pub target_name: String,

    /// FK to [`RankDefinition::id`].
    #[index(btree)]
    pub rank_id: String,

    pub executor_uuid: PlayerUuid,
    pub executor_name: String,

    pub reason: String,

    /// `STAFF`, `PURCHASE`, `PROMOTION`, `SYSTEM`. See
    /// [`crate::common::grant_source`].
    pub source: String,

    pub created_at: Timestamp,

    /// `0` = permanent.
    pub expires_at_micros: i64,

    /// Active grants count toward the player's rank set; inactive grants
    /// remain in history (PLAN line 683). Soft-deactivate via
    /// `grant_deactivate`; hard-remove via `grant_remove_inactive`.
    #[index(btree)]
    pub active: bool,
}

// ── Helpers exposed for the proxy / suite ────────────────────────────

/// Find the highest-priority active rank id for a player. Lowest weight
/// wins; ties broken by `created_at` (newest first), then by rank id for
/// stability. Returns `None` if no active grants — caller decides
/// fallback (mythic-core uses `"default"`).
pub fn active_rank(ctx: &ReducerContext, uuid: &str) -> Option<String> {
    let now = ctx.timestamp.to_micros_since_unix_epoch();
    let definitions = ctx.db.rank_definitions();
    let mut best: Option<(i32, i64, String)> = None;
    for grant in ctx.db.rank_grants().iter() {
        if grant.target_uuid != uuid || !grant.active {
            continue;
        }
        if grant.expires_at_micros != 0 && grant.expires_at_micros <= now {
            continue;
        }
        let Some(def) = definitions.id().find(grant.rank_id.clone()) else {
            continue;
        };
        let key = (
            def.weight,
            -grant.created_at.to_micros_since_unix_epoch(),
            grant.rank_id.clone(),
        );
        match &best {
            None => best = Some(key),
            Some(current) if key < *current => best = Some(key),
            _ => {}
        }
    }
    best.map(|(_, _, id)| id)
}

// ── Reducers: definitions ────────────────────────────────────────────

/// Insert-or-update a rank definition. Idempotent — safe to call from
/// YAML seeding on every boot.
#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn rank_define(
    ctx: &ReducerContext,
    id: String,
    display_name: String,
    color: String,
    dye: String,
    prefix: String,
    suffix: String,
    weight: i32,
    staff: bool,
    donator: bool,
    parent: String,
    permissions_json: String,
    chat_prefix: String,
    chat_format: String,
    tab_prefix: String,
    tab_format: String,
    nametag_prefix: String,
    nametag_format: String,
    seeded: bool,
) -> ReducerResult {
    require_backend(ctx)?;
    if id.is_empty() {
        reject!("rank id required");
    }
    let id = id.to_lowercase();
    let definitions = ctx.db.rank_definitions();
    if let Some(mut def) = definitions.id().find(id.clone()) {
        def.display_name = display_name;
        def.color = color;
        def.dye = dye;
        def.prefix = prefix;
        def.suffix = suffix;
        def.weight = weight;
        def.staff = staff;
        def.donator = donator;
        def.parent = parent;
        def.permissions_json = permissions_json;
        def.chat_prefix = chat_prefix;
        def.chat_format = chat_format;
        def.tab_prefix = tab_prefix;
        def.tab_format = tab_format;
        def.nametag_prefix = nametag_prefix;
        def.nametag_format = nametag_format;
        if seeded {
            def.seeded = true;
        }
        def.updated_at = ctx.timestamp;
        definitions.id().update(def);
    } else {
        definitions.insert(RankDefinition {
            id,
            display_name,
            color,
            dye,
            prefix,
            suffix,
            weight,
            staff,
            donator,
            parent,
            permissions_json,
            chat_prefix,
            chat_format,
            tab_prefix,
            tab_format,
            nametag_prefix,
            nametag_format,
            seeded,
            created_at: ctx.timestamp,
            updated_at: ctx.timestamp,
        });
    }
    Ok(())
}

#[reducer]
pub fn rank_remove(ctx: &ReducerContext, id: String) -> ReducerResult {
    require_backend(ctx)?;
    let id = id.to_lowercase();
    if id == "default" {
        reject!("cannot remove the default rank");
    }
    // Refuse if any active grant references this rank — the caller should
    // call `grant_clear` for affected players first.
    let in_use = ctx
        .db
        .rank_grants()
        .iter()
        .any(|g| g.rank_id == id && g.active);
    if in_use {
        reject!("rank {id} still has active grants; clear them first");
    }
    ctx.db.rank_definitions().id().delete(id);
    Ok(())
}

// ── Reducers: grants ─────────────────────────────────────────────────

#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn grant_issue(
    ctx: &ReducerContext,
    target_uuid: PlayerUuid,
    target_name: String,
    rank_id: String,
    executor_uuid: PlayerUuid,
    executor_name: String,
    reason: String,
    source: String,
    duration_seconds: i64,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&target_uuid)?;
    let rank_id = rank_id.to_lowercase();
    if ctx.db.rank_definitions().id().find(rank_id.clone()).is_none() {
        reject!("unknown rank: {rank_id}");
    }
    let expires_at_micros = if duration_seconds <= 0 {
        0
    } else {
        ctx.timestamp.to_micros_since_unix_epoch() + duration_seconds * 1_000_000
    };
    ctx.db.rank_grants().insert(RankGrant {
        id: 0,
        target_uuid,
        target_name,
        rank_id,
        executor_uuid,
        executor_name,
        reason,
        source,
        created_at: ctx.timestamp,
        expires_at_micros,
        active: true,
    });
    Ok(())
}

/// Soft-deactivate a grant — keeps the row visible in history.
/// Used by `/grants` left-click and by the expiry janitor.
#[reducer]
pub fn grant_deactivate(ctx: &ReducerContext, grant_id: u64) -> ReducerResult {
    require_backend(ctx)?;
    let grants = ctx.db.rank_grants();
    let Some(mut grant) = grants.id().find(grant_id) else {
        reject!("grant {grant_id} not found");
    };
    if !grant.active {
        return Ok(());
    }
    grant.active = false;
    grants.id().update(grant);
    Ok(())
}

/// Hard-remove an inactive grant from history. `/grants` right-click on
/// inactive entries. Refuses to remove active grants — caller must
/// `grant_deactivate` first.
#[reducer]
pub fn grant_remove_inactive(ctx: &ReducerContext, grant_id: u64) -> ReducerResult {
    require_backend(ctx)?;
    let grants = ctx.db.rank_grants();
    let Some(grant) = grants.id().find(grant_id) else {
        reject!("grant {grant_id} not found");
    };
    if grant.active {
        reject!("grant {grant_id} is still active; deactivate first");
    }
    grants.id().delete(grant_id);
    Ok(())
}

/// Clear all grants for a player — both active history and inactive.
/// `/cleargrants <username>`.
#[reducer]
pub fn grant_clear(ctx: &ReducerContext, target_uuid: PlayerUuid) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&target_uuid)?;
    let grants = ctx.db.rank_grants();
    let ids: Vec<u64> = grants
        .iter()
        .filter(|g| g.target_uuid == target_uuid)
        .map(|g| g.id)
        .collect();
    let n = ids.len();
    for id in ids {
        grants.id().delete(id);
    }
    log::info!("grant_clear removed {n} grants for {target_uuid}");
    Ok(())
}

/// Janitor: deactivate grants whose `expires_at_micros` has passed.
/// Cron-style — call from the proxy on an interval.
#[reducer]
pub fn grant_expire(ctx: &ReducerContext) -> ReducerResult {
    require_backend(ctx)?;
    let now = ctx.timestamp.to_micros_since_unix_epoch();
    let grants = ctx.db.rank_grants();
    let to_expire: Vec<RankGrant> = grants
        .iter()
        .filter(|g| g.active && g.expires_at_micros != 0 && g.expires_at_micros <= now)
        .collect();
    let n = to_expire.len();
    for mut g in to_expire {
        g.active = false;
        grants.id().update(g);
    }
    log::info!("grant_expire deactivated {n} expired grants");
    Ok(())
}

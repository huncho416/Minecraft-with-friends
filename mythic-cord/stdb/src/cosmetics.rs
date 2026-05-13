//! Cosmetic ownership and equipped loadout.
//!
//! Cosmetics are EULA-safe: hats, titles, particles, chat tags. Catalog
//! definitions (display name, model id, rarity, source) live in YAML in the
//! suite — STDB only stores ownership and equip state.

use crate::common::{require_backend, require_uuid, PlayerUuid, ReducerResult};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = cosmetic_grants, public)]
pub struct CosmeticGrant {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    /// Catalog id (e.g. `hat.party_crown`).
    #[index(btree)]
    pub cosmetic_id: String,

    /// One of [`cosmetic_type`] constants.
    #[index(btree)]
    pub cosmetic_type: String,

    /// `TEBEX`, `EVENT`, `STAFF_GRANT`, `ACHIEVEMENT`.
    pub source: String,

    /// Optional reference (Tebex tx id, achievement id, …).
    pub reference: String,

    pub granted_at: Timestamp,
}

/// One row per (player, cosmetic_type) — the currently equipped item for
/// that slot. Empty `cosmetic_id` = nothing equipped.
#[table(name = cosmetic_equipped, public)]
pub struct EquippedSlot {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    #[index(btree)]
    pub cosmetic_type: String,

    pub cosmetic_id: String,

    pub equipped_at: Timestamp,
}

// ── Reducers ──────────────────────────────────────────────────────────

#[reducer]
pub fn cosmetic_grant(
    ctx: &ReducerContext,
    player_uuid: PlayerUuid,
    cosmetic_id: String,
    cosmetic_type: String,
    source: String,
    reference: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&player_uuid)?;
    if cosmetic_id.is_empty() {
        reject!("cosmetic_id required");
    }
    // Idempotent: don't double-grant the same id to the same player.
    let already = ctx
        .db
        .cosmetic_grants()
        .iter()
        .any(|g| g.player_uuid == player_uuid && g.cosmetic_id == cosmetic_id);
    if already {
        return Ok(());
    }
    ctx.db.cosmetic_grants().insert(CosmeticGrant {
        id: 0,
        player_uuid,
        cosmetic_id,
        cosmetic_type,
        source,
        reference,
        granted_at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn cosmetic_equip(
    ctx: &ReducerContext,
    player_uuid: PlayerUuid,
    cosmetic_type: String,
    cosmetic_id: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&player_uuid)?;

    // Verify ownership unless unequipping (empty id).
    if !cosmetic_id.is_empty() {
        let owned = ctx
            .db
            .cosmetic_grants()
            .iter()
            .any(|g| g.player_uuid == player_uuid && g.cosmetic_id == cosmetic_id);
        if !owned {
            reject!("{player_uuid} does not own {cosmetic_id}");
        }
    }

    let slots = ctx.db.cosmetic_equipped();
    let existing: Option<EquippedSlot> = slots
        .iter()
        .find(|s| s.player_uuid == player_uuid && s.cosmetic_type == cosmetic_type);
    if let Some(mut s) = existing {
        s.cosmetic_id = cosmetic_id;
        s.equipped_at = ctx.timestamp;
        slots.id().update(s);
    } else {
        slots.insert(EquippedSlot {
            id: 0,
            player_uuid,
            cosmetic_type,
            cosmetic_id,
            equipped_at: ctx.timestamp,
        });
    }
    Ok(())
}



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

    #[index(btree)]
    pub cosmetic_id: String,

    #[index(btree)]
    pub cosmetic_type: String,

    pub source: String,

    pub reference: String,

    pub granted_at: Timestamp,
}

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

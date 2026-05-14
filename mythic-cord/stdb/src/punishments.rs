

use crate::common::{
    punishment_category, punishment_kind, require_backend, require_uuid, PlayerUuid, ReducerResult,
};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = punishments, public)]
pub struct Punishment {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub target_uuid: PlayerUuid,

    pub target_name: String,

    #[index(btree)]
    pub staff_uuid: PlayerUuid,

    pub staff_name: String,

    #[index(btree)]
    pub kind: String,

    pub reason: String,

    pub proof: String,

    pub issued_at: Timestamp,

    pub expires_at_micros: i64,

    #[index(btree)]
    pub active: bool,

    pub silent: bool,

    pub clear_inventory: bool,

    pub server: String,

    pub pardoned_by: PlayerUuid,
    pub pardoned_at_micros: i64,
    pub pardon_reason: String,
}

#[table(name = punishment_appeals, public)]
pub struct Appeal {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub punishment_id: u64,

    #[index(btree)]
    pub target_uuid: PlayerUuid,

    pub message: String,

    #[index(btree)]
    pub status: String,

    pub reviewer_uuid: PlayerUuid,
    pub review_notes: String,

    pub created_at: Timestamp,
    pub reviewed_at_micros: i64,
}

#[table(name = punishment_templates, public)]
pub struct PunishmentTemplate {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub title: String,

    #[index(btree)]
    pub category: String,

    pub duration: String,

    pub information: String,

    pub seeded: bool,

    pub created_at: Timestamp,
    pub updated_at: Timestamp,
}

#[table(name = punishment_blacklist, public)]
pub struct BlacklistEntry {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub target_uuid: PlayerUuid,

    pub target_name: String,

    pub staff_uuid: PlayerUuid,
    pub staff_name: String,

    pub reason: String,

    #[index(btree)]
    pub active: bool,

    pub created_at: Timestamp,

    pub revoked_at_micros: i64,
    pub revoked_by: PlayerUuid,
    pub revoke_reason: String,
}

pub fn has_active(ctx: &ReducerContext, uuid: &str, kind: &str) -> bool {
    let now = ctx.timestamp.to_micros_since_unix_epoch();
    ctx.db
        .punishments()
        .iter()
        .any(|p| p.target_uuid == uuid && p.active && p.kind == kind && {

            p.expires_at_micros == 0 || p.expires_at_micros > now
        })
}

pub fn is_blacklisted(ctx: &ReducerContext, uuid: &str) -> bool {
    ctx.db
        .punishment_blacklist()
        .iter()
        .any(|b| b.target_uuid == uuid && b.active)
}

#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn punish_issue(
    ctx: &ReducerContext,
    target_uuid: PlayerUuid,
    target_name: String,
    staff_uuid: PlayerUuid,
    staff_name: String,
    kind: String,
    reason: String,
    proof: String,
    duration_seconds: i64,
    silent: bool,
    clear_inventory: bool,
    server: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&target_uuid)?;
    if !punishment_kind::is_valid(&kind) {
        reject!("invalid punishment kind: {kind}");
    }
    let expires_at_micros = if duration_seconds <= 0 {
        0
    } else {
        ctx.timestamp.to_micros_since_unix_epoch() + duration_seconds * 1_000_000
    };
    ctx.db.punishments().insert(Punishment {
        id: 0,
        target_uuid,
        target_name,
        staff_uuid,
        staff_name,
        kind,
        reason,
        proof,
        issued_at: ctx.timestamp,
        expires_at_micros,
        active: true,
        silent,
        clear_inventory,
        server,
        pardoned_by: String::new(),
        pardoned_at_micros: 0,
        pardon_reason: String::new(),
    });
    Ok(())
}

#[reducer]
pub fn punish_pardon(
    ctx: &ReducerContext,
    punishment_id: u64,
    staff_uuid: PlayerUuid,
    reason: String,
) -> ReducerResult {
    require_backend(ctx)?;
    let punishments = ctx.db.punishments();
    let Some(mut p) = punishments.id().find(punishment_id) else {
        reject!("punishment {punishment_id} not found");
    };
    if !p.active {
        reject!("punishment {punishment_id} already inactive");
    }
    p.active = false;
    p.pardoned_by = staff_uuid;
    p.pardoned_at_micros = ctx.timestamp.to_micros_since_unix_epoch();
    p.pardon_reason = reason;
    punishments.id().update(p);
    Ok(())
}

#[reducer]
pub fn punish_expire(ctx: &ReducerContext) -> ReducerResult {
    require_backend(ctx)?;
    let now = ctx.timestamp.to_micros_since_unix_epoch();
    let punishments = ctx.db.punishments();
    let to_expire: Vec<Punishment> = punishments
        .iter()
        .filter(|p| p.active && p.expires_at_micros != 0 && p.expires_at_micros <= now)
        .collect();
    let n = to_expire.len();
    for mut p in to_expire {
        p.active = false;
        p.pardoned_by = "SYSTEM".to_string();
        p.pardoned_at_micros = now;
        p.pardon_reason = "expired".to_string();
        punishments.id().update(p);
    }
    log::info!("punish_expire flipped {n} rows");
    Ok(())
}

#[reducer]
pub fn punish_clear_history(
    ctx: &ReducerContext,
    target_uuid: PlayerUuid,
    staff_uuid: PlayerUuid,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&target_uuid)?;
    let now = ctx.timestamp.to_micros_since_unix_epoch();
    let punishments = ctx.db.punishments();
    let to_clear: Vec<Punishment> = punishments
        .iter()
        .filter(|p| p.target_uuid == target_uuid && p.active)
        .collect();
    let n = to_clear.len();
    for mut p in to_clear {
        p.active = false;
        p.pardoned_by = staff_uuid.clone();
        p.pardoned_at_micros = now;
        p.pardon_reason = "history cleared".to_string();
        punishments.id().update(p);
    }
    log::info!("punish_clear_history cleared {n} rows for {target_uuid}");
    Ok(())
}

#[reducer]
pub fn template_upsert(
    ctx: &ReducerContext,
    title: String,
    category: String,
    duration: String,
    information: String,
    seeded: bool,
) -> ReducerResult {
    require_backend(ctx)?;
    if title.is_empty() {
        reject!("template title required");
    }
    if !punishment_category::is_valid(&category) {
        reject!("invalid punishment category: {category}");
    }
    let templates = ctx.db.punishment_templates();

    let existing: Option<PunishmentTemplate> =
        templates.iter().find(|t| t.title.eq_ignore_ascii_case(&title));
    if let Some(mut t) = existing {
        t.category = category;
        t.duration = duration;
        t.information = information;

        if seeded {
            t.seeded = true;
        }
        t.updated_at = ctx.timestamp;
        templates.id().update(t);
    } else {
        templates.insert(PunishmentTemplate {
            id: 0,
            title,
            category,
            duration,
            information,
            seeded,
            created_at: ctx.timestamp,
            updated_at: ctx.timestamp,
        });
    }
    Ok(())
}

#[reducer]
pub fn template_remove(ctx: &ReducerContext, title: String) -> ReducerResult {
    require_backend(ctx)?;
    let templates = ctx.db.punishment_templates();
    let target = templates
        .iter()
        .find(|t| t.title.eq_ignore_ascii_case(&title));
    let Some(t) = target else {
        reject!("template {title} not found");
    };
    let id = t.id;
    templates.id().delete(id);
    Ok(())
}

#[reducer]
pub fn blacklist_add(
    ctx: &ReducerContext,
    target_uuid: PlayerUuid,
    target_name: String,
    staff_uuid: PlayerUuid,
    staff_name: String,
    reason: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&target_uuid)?;

    if is_blacklisted(ctx, &target_uuid) {
        return Ok(());
    }
    ctx.db.punishment_blacklist().insert(BlacklistEntry {
        id: 0,
        target_uuid,
        target_name,
        staff_uuid,
        staff_name,
        reason,
        active: true,
        created_at: ctx.timestamp,
        revoked_at_micros: 0,
        revoked_by: String::new(),
        revoke_reason: String::new(),
    });
    Ok(())
}

#[reducer]
pub fn blacklist_revoke(
    ctx: &ReducerContext,
    entry_id: u64,
    staff_uuid: PlayerUuid,
    reason: String,
) -> ReducerResult {
    require_backend(ctx)?;
    let blacklist = ctx.db.punishment_blacklist();
    let Some(mut entry) = blacklist.id().find(entry_id) else {
        reject!("blacklist entry {entry_id} not found");
    };
    if !entry.active {
        reject!("blacklist entry {entry_id} already inactive");
    }
    entry.active = false;
    entry.revoked_by = staff_uuid;
    entry.revoked_at_micros = ctx.timestamp.to_micros_since_unix_epoch();
    entry.revoke_reason = reason;
    blacklist.id().update(entry);
    Ok(())
}

#[reducer]
pub fn appeal_open(
    ctx: &ReducerContext,
    punishment_id: u64,
    target_uuid: PlayerUuid,
    message: String,
) -> ReducerResult {
    require_uuid(&target_uuid)?;
    let punishments = ctx.db.punishments();
    let Some(p) = punishments.id().find(punishment_id) else {
        reject!("punishment {punishment_id} not found");
    };
    if p.target_uuid != target_uuid {
        reject!("punishment {punishment_id} does not belong to {target_uuid}");
    }
    ctx.db.punishment_appeals().insert(Appeal {
        id: 0,
        punishment_id,
        target_uuid,
        message,
        status: "OPEN".to_string(),
        reviewer_uuid: String::new(),
        review_notes: String::new(),
        created_at: ctx.timestamp,
        reviewed_at_micros: 0,
    });
    Ok(())
}

#[reducer]
pub fn appeal_review(
    ctx: &ReducerContext,
    appeal_id: u64,
    reviewer_uuid: PlayerUuid,
    decision: String,
    notes: String,
) -> ReducerResult {
    require_backend(ctx)?;
    if !matches!(decision.as_str(), "APPROVED" | "DENIED") {
        reject!("decision must be APPROVED or DENIED, got {decision}");
    }
    let appeals = ctx.db.punishment_appeals();
    let Some(mut a) = appeals.id().find(appeal_id) else {
        reject!("appeal {appeal_id} not found");
    };
    if a.status != "OPEN" {
        reject!("appeal {appeal_id} already {}", a.status);
    }
    a.status = decision.clone();
    a.reviewer_uuid = reviewer_uuid.clone();
    a.review_notes = notes;
    a.reviewed_at_micros = ctx.timestamp.to_micros_since_unix_epoch();
    let punishment_id = a.punishment_id;
    appeals.id().update(a);

    if decision == "APPROVED" {
        punish_pardon(
            ctx,
            punishment_id,
            reviewer_uuid,
            "appeal approved".to_string(),
        )?;
    }
    Ok(())
}

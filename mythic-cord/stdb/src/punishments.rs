//! Punishments — bans, mutes, kicks, warns + appeals.
//!
//! Active vs historical:
//! - The `punishments` table is the **full audit log**. Rows are never
//!   deleted; expiry/pardon flips `active=false` and stamps a reason.
//! - Lookups for "is this player currently banned?" use the
//!   `(target_uuid, active)` index.

use crate::common::{punishment_kind, require_backend, require_uuid, PlayerUuid, ReducerResult};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = punishments, public)]
pub struct Punishment {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    /// Player on the receiving end.
    #[index(btree)]
    pub target_uuid: PlayerUuid,

    /// Staff member who issued it. `SYSTEM` for automated actions.
    #[index(btree)]
    pub staff_uuid: PlayerUuid,

    /// One of [`punishment_kind`] constants.
    #[index(btree)]
    pub kind: String,

    pub reason: String,

    /// Free-form evidence link (screenshot URL, log id, etc.).
    pub evidence: String,

    pub issued_at: Timestamp,

    /// `None` = permanent. SpacetimeDB doesn't allow Option<Timestamp> in
    /// some versions — we use `0` as the sentinel for "no expiry".
    pub expires_at_micros: i64,

    /// Whether this punishment is currently in effect. Indexed alongside
    /// `target_uuid` for cheap "active bans for player" subscriptions.
    #[index(btree)]
    pub active: bool,

    /// If pardoned/appealed, who and when.
    pub pardoned_by: PlayerUuid,
    pub pardoned_at_micros: i64,
    pub pardon_reason: String,
}

#[table(name = punishment_appeals, public)]
pub struct Appeal {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    /// FK to [`Punishment::id`].
    #[index(btree)]
    pub punishment_id: u64,

    #[index(btree)]
    pub target_uuid: PlayerUuid,

    pub message: String,

    /// `OPEN`, `APPROVED`, `DENIED`, `WITHDRAWN`.
    #[index(btree)]
    pub status: String,

    pub reviewer_uuid: PlayerUuid,
    pub review_notes: String,

    pub created_at: Timestamp,
    pub reviewed_at_micros: i64,
}

// ── Helpers ───────────────────────────────────────────────────────────

/// Returns `true` if the player has any active punishment of `kind`.
/// Used by the proxy on login (`PERMA_BAN`/`TEMP_BAN`) and by the chat
/// module on send (`MUTE`).
pub fn has_active(ctx: &ReducerContext, uuid: &str, kind: &str) -> bool {
    let now = ctx.timestamp.to_micros_since_unix_epoch();
    ctx.db
        .punishments()
        .iter()
        .any(|p| p.target_uuid == uuid && p.active && p.kind == kind && {
            // Auto-expire is checked here; the janitor reducer does the
            // actual flip later.
            p.expires_at_micros == 0 || p.expires_at_micros > now
        })
}

// ── Reducers ──────────────────────────────────────────────────────────

#[reducer]
pub fn punish_issue(
    ctx: &ReducerContext,
    target_uuid: PlayerUuid,
    staff_uuid: PlayerUuid,
    kind: String,
    reason: String,
    evidence: String,
    duration_seconds: i64,
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
        staff_uuid,
        kind,
        reason,
        evidence,
        issued_at: ctx.timestamp,
        expires_at_micros,
        active: true,
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

/// Janitor: flip `active=false` on rows whose `expires_at_micros` has passed.
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

// ── Appeals ──────────────────────────────────────────────────────────

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

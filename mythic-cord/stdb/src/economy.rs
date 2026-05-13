//! Economy: append-only transaction log + denormalized balances.
//!
//! Source-of-truth balances live on the `players` row (see
//! [`crate::players::adjust_balance`]). The transaction log here exists for:
//! - **Audit** — every mutation is attributable.
//! - **Rollback** — `economy_rollback` reverses all transactions in a
//!   time window for a player or globally (used for dupe incidents).
//! - **Anomaly detection** — downstream jobs flag balance changes that
//!   exceed configured σ thresholds.
//!
//! We never delete transaction rows. Cold archival is a separate pipeline.

use crate::common::{currency, require_backend, require_uuid, PlayerUuid, ReducerResult};
use crate::players::adjust_balance;
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = transactions, public)]
pub struct Transaction {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    /// One of [`currency`] constants.
    #[index(btree)]
    pub currency: String,

    /// Signed delta. Positive = credit, negative = debit.
    pub amount: i64,

    /// Balance after this txn — handy for reconciliation reports.
    pub balance_after: i64,

    /// Free-form source tag. Conventional values:
    /// `KILL_REWARD`, `SHOP_BUY`, `SHOP_SELL`, `STAFF_GRANT`, `TEBEX`,
    /// `ROLLBACK`, `QUEST`, `EVENT`, `INTEREST`.
    #[index(btree)]
    pub source: String,

    /// Free-form correlator (shop item id, quest id, tebex tx id, …).
    pub reference: String,

    /// `true` if this row was inserted as part of a rollback. These are
    /// excluded from rollback windows to prevent recursive reversal.
    pub is_rollback: bool,

    pub at: Timestamp,
}

// ── Reducers ──────────────────────────────────────────────────────────

/// Credit `amount` of `currency` to `uuid`. Negative `amount` debits.
/// Atomically updates the denormalized balance and appends a txn row.
#[reducer]
pub fn economy_adjust(
    ctx: &ReducerContext,
    uuid: PlayerUuid,
    currency_code: String,
    amount: i64,
    source: String,
    reference: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    if !currency::is_valid(&currency_code) {
        reject!("invalid currency: {currency_code}");
    }
    if amount == 0 {
        reject!("amount must be non-zero");
    }
    let new_balance = adjust_balance(ctx, &uuid, &currency_code, amount)?;
    ctx.db.transactions().insert(Transaction {
        id: 0,
        player_uuid: uuid,
        currency: currency_code,
        amount,
        balance_after: new_balance,
        source,
        reference,
        is_rollback: false,
        at: ctx.timestamp,
    });
    Ok(())
}

/// Atomic transfer between two players (e.g. trade, /pay).
/// Both legs succeed or both fail.
#[reducer]
pub fn economy_transfer(
    ctx: &ReducerContext,
    from_uuid: PlayerUuid,
    to_uuid: PlayerUuid,
    currency_code: String,
    amount: i64,
    reference: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&from_uuid)?;
    require_uuid(&to_uuid)?;
    if from_uuid == to_uuid {
        reject!("cannot transfer to self");
    }
    if amount <= 0 {
        reject!("amount must be positive");
    }
    if !currency::is_valid(&currency_code) {
        reject!("invalid currency: {currency_code}");
    }
    // Order matters: debit first so insufficient-funds rejects atomically.
    let from_balance = adjust_balance(ctx, &from_uuid, &currency_code, -amount)?;
    let to_balance = adjust_balance(ctx, &to_uuid, &currency_code, amount)?;
    let now = ctx.timestamp;
    let txns = ctx.db.transactions();
    txns.insert(Transaction {
        id: 0,
        player_uuid: from_uuid.clone(),
        currency: currency_code.clone(),
        amount: -amount,
        balance_after: from_balance,
        source: "TRANSFER_OUT".to_string(),
        reference: format!("to={to_uuid} {reference}"),
        is_rollback: false,
        at: now,
    });
    txns.insert(Transaction {
        id: 0,
        player_uuid: to_uuid.clone(),
        currency: currency_code,
        amount,
        balance_after: to_balance,
        source: "TRANSFER_IN".to_string(),
        reference: format!("from={from_uuid} {reference}"),
        is_rollback: false,
        at: now,
    });
    Ok(())
}

/// Reverse every non-rollback transaction for `uuid` in
/// `[since_micros, until_micros]` (inclusive). Each reversal appends a
/// new txn row tagged `is_rollback=true`.
///
/// Performance note: this is O(N) over `transactions` for the player.
/// Acceptable since rollbacks are operator-triggered and rare. If we
/// outgrow it, add a (player_uuid, at) composite index.
#[reducer]
pub fn economy_rollback(
    ctx: &ReducerContext,
    uuid: PlayerUuid,
    since_micros: i64,
    until_micros: i64,
    reason: String,
) -> ReducerResult {
    require_backend(ctx)?;
    require_uuid(&uuid)?;
    if since_micros >= until_micros {
        reject!("since must be < until");
    }
    let txns = ctx.db.transactions();
    let in_window: Vec<Transaction> = txns
        .iter()
        .filter(|t| {
            t.player_uuid == uuid
                && !t.is_rollback
                && t.at.to_micros_since_unix_epoch() >= since_micros
                && t.at.to_micros_since_unix_epoch() <= until_micros
        })
        .collect();

    let n = in_window.len();
    for t in in_window {
        let new_balance = adjust_balance(ctx, &t.player_uuid, &t.currency, -t.amount)?;
        txns.insert(Transaction {
            id: 0,
            player_uuid: t.player_uuid.clone(),
            currency: t.currency.clone(),
            amount: -t.amount,
            balance_after: new_balance,
            source: "ROLLBACK".to_string(),
            reference: format!("orig_id={} {reason}", t.id),
            is_rollback: true,
            at: ctx.timestamp,
        });
    }
    log::warn!("economy_rollback reversed {n} txns for {uuid}");
    Ok(())
}

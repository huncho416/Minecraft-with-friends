//! MythicPvP SpacetimeDB module.
//!
//! Schema and reducers consumed by **both** the Rust proxy (MythicCord) and
//! the Java game-server suite. This crate compiles to `wasm32-unknown-unknown`
//! and is published with `spacetime publish mythicpvp`.
//!
//! ### Module layout
//! - [`common`] — shared types, identifiers, error/result helpers
//! - [`players`] — `players` table + identity reducers
//! - [`registry`] — `server_registry` table (proxy↔shard discovery & health)
//! - [`sessions`] — `sessions` table + login/logout/route reducers
//! - [`punishments`] — bans/mutes/kicks/warns + appeals
//! - [`economy`] — balances + transaction log + rollback reducer
//! - [`social`] — friends, party, mail
//! - [`cosmetics`] — owned/equipped cosmetics
//! - [`gameplay`] — islands, skills, stats, leaderboards
//!
//! ### Conventions
//! - **Timestamps** are `spacetimedb::Timestamp` (microseconds since epoch).
//! - **Player identity** is `Uuid` (Mojang UUID) stored as `String` for
//!   client-side ergonomics. We don't use `Identity` because Minecraft players
//!   authenticate against Mojang, not SpacetimeDB.
//! - **Reducer naming**: `<noun>_<verb>` (e.g. `player_login`, `economy_grant`).
//! - **Indexes**: every table that's looked up by non-PK column declares a
//!   `#[index(btree)]` so subscriptions stay cheap as row counts grow.
//! - **Schema migrations**: bump [`SCHEMA_VERSION`] and add a one-shot
//!   migration reducer that callers can invoke during deploy.

// Reducers in SpacetimeDB take owned args by macro convention.
#![allow(clippy::needless_pass_by_value)]
// Proper nouns ("SpacetimeDB", "MythicCord", etc.) appear in module docs.
#![allow(clippy::doc_markdown)]
// Many reducers do `target.field = src.clone()` where `src` is still used
// downstream — `clone_into` would force premature ownership transfer.
#![allow(clippy::assigning_clones)]
// `load_score` and the XP curve placeholder cast u32/u64 → f32/f64 by
// design; we only need monotonic ordering, not exact representation.
#![allow(clippy::cast_precision_loss)]
// Identical match arms in `leaderboard_rebuild` (MONTHLY uses weekly bucket
// until a monthly bucket exists) and a few other intentional placeholders.
#![allow(clippy::match_same_arms)]
// `uuid.as_bytes().iter().filter(...).count()` is plenty fast for 36 bytes;
// the bytecount crate isn't worth the dep.
#![allow(clippy::naive_bytecount)]
// `if cond { 0 } else { 1 }` is clearer than `u32::from(!cond)` in routing
// tie-breakers (we already use the latter where it reads naturally).
#![allow(clippy::bool_to_int_with_if)]
// XP curve uses `_ as u32` after `.floor()`; bounded by data so safe.
// Leaderboard rank uses `i as u32` where `i < top_n: u32`.
#![allow(clippy::cast_possible_truncation)]
// Same XP curve: `_ as u32` from sqrt() of non-negative f64 can't be signed.
#![allow(clippy::cast_sign_loss)]

pub mod common;
pub mod cosmetics;
pub mod economy;
pub mod gameplay;
pub mod players;
pub mod punishments;
pub mod registry;
pub mod sessions;
pub mod social;

use spacetimedb::{reducer, table, ReducerContext, Table};

/// Bump on every backward-incompatible schema change.
/// The Java suite reads this at boot and refuses to start on mismatch.
pub const SCHEMA_VERSION: u32 = 1;

/// Singleton row holding module-wide metadata.
#[table(name = module_meta, public)]
pub struct ModuleMeta {
    /// Always `0` — this table holds exactly one row.
    #[primary_key]
    pub id: u8,
    /// Current schema version, mirrors [`SCHEMA_VERSION`].
    pub schema_version: u32,
    /// When the module was first published.
    pub initialized_at: spacetimedb::Timestamp,
    /// Last reducer that mutated the meta row.
    pub last_migrated_at: spacetimedb::Timestamp,
}

/// One-time initializer. SpacetimeDB invokes `init` on first publish; on
/// subsequent publishes we just upsert the schema version.
#[reducer(init)]
pub fn init(ctx: &ReducerContext) {
    let now = ctx.timestamp;
    ctx.db.module_meta().insert(ModuleMeta {
        id: 0,
        schema_version: SCHEMA_VERSION,
        initialized_at: now,
        last_migrated_at: now,
    });
    log::info!("mythic-stdb initialized at v{SCHEMA_VERSION}");
}

/// Called on every client connect — used by [`sessions`] to mark presence.
#[reducer(client_connected)]
pub fn on_connect(ctx: &ReducerContext) {
    sessions::on_client_connected(ctx);
}

/// Called on every client disconnect.
#[reducer(client_disconnected)]
pub fn on_disconnect(ctx: &ReducerContext) {
    sessions::on_client_disconnected(ctx);
}

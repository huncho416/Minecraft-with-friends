

#![allow(clippy::needless_pass_by_value)]

#![allow(clippy::doc_markdown)]

#![allow(clippy::assigning_clones)]

#![allow(clippy::cast_precision_loss)]

#![allow(clippy::match_same_arms)]

#![allow(clippy::naive_bytecount)]

#![allow(clippy::bool_to_int_with_if)]

#![allow(clippy::cast_possible_truncation)]

#![allow(clippy::cast_sign_loss)]

pub mod common;
pub mod cosmetics;
pub mod economy;
pub mod gameplay;
pub mod players;
pub mod punishments;
pub mod ranks;
pub mod registry;
pub mod sessions;
pub mod social;

use spacetimedb::{reducer, table, ReducerContext, Table};

pub const SCHEMA_VERSION: u32 = 2;

#[table(name = module_meta, public)]
pub struct ModuleMeta {

    #[primary_key]
    pub id: u8,

    pub schema_version: u32,

    pub initialized_at: spacetimedb::Timestamp,

    pub last_migrated_at: spacetimedb::Timestamp,
}

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

#[reducer(client_connected)]
pub fn on_connect(ctx: &ReducerContext) {
    sessions::on_client_connected(ctx);
}

#[reducer(client_disconnected)]
pub fn on_disconnect(ctx: &ReducerContext) {
    sessions::on_client_disconnected(ctx);
}

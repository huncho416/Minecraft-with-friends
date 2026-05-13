//! MythicCord routing plugin.
//!
//! Responsibilities:
//! 1. **Lifecycle → STDB** — translate Infrarust's `PreLoginEvent` /
//!    `PostLoginEvent` / `DisconnectEvent` into `session_login` /
//!    `session_logout` reducer calls.
//! 2. **Routing decision** — read the `server_registry` table and pick
//!    the best target shard for each player using the same `pick_shard`
//!    algorithm the Rust schema exports.
//! 3. **Heartbeat** — call `registry_heartbeat` on a tokio interval so
//!    other proxies and dashboards see the proxy as alive.
//! 4. **Drain coordination** — on SIGTERM, call `registry_drain` first,
//!    refuse new logins, then wait for active sessions to bleed out
//!    before exiting (Pterodactyl-friendly graceful shutdown).
//!
//! Infrarust integration lives in [`integration`] behind the
//! `with-infrarust` feature. The schema-only logic in [`router`] and
//! [`heartbeat`] compiles standalone and is unit-testable without the
//! upstream subtree.

#![allow(clippy::module_name_repetitions)]

pub mod heartbeat;
pub mod registry_view;
pub mod router;

#[cfg(feature = "with-infrarust")]
pub mod integration;

use mythiccord_stdb_bridge::{MythicStdbClient, ServerRole, ServerStatus};
use std::sync::Arc;

/// Per-proxy identity and runtime config.
#[derive(Debug, Clone)]
pub struct ProxyIdentity {
    /// Globally-unique shard id for this proxy instance.
    pub shard_id: String,
    pub role: ServerRole,
    pub region: String,
    /// Address backends (or sibling proxies) would dial to reach us.
    pub address: String,
    pub max_players: u32,
}

/// Runtime context passed through the plugin.
#[derive(Clone)]
pub struct RoutingRuntime {
    pub identity: ProxyIdentity,
    pub stdb: Arc<MythicStdbClient>,
    /// Shared snapshot of the server registry, kept current by the
    /// `registry_view` task. Cheap to clone — backed by an `ArcSwap`.
    pub registry: registry_view::RegistryView,
    /// Current proxy status. Flipped to [`ServerStatus::Draining`] on
    /// SIGTERM by the proxy's signal handler.
    pub status: Arc<parking_lot::RwLock<ServerStatus>>,
}

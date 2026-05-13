//! Shared proxy state. One instance per proxy process, wrapped in `Arc`
//! so the admin server, signal handler, and (future) Infrarust plugin
//! all see the same status flag.

use mythiccord_plugin_routing::registry_view::RegistryView;
use mythiccord_plugin_routing::ProxyIdentity;
use mythiccord_stdb_bridge::{MythicStdbClient, ServerStatus};
use parking_lot::RwLock;
use std::sync::Arc;

pub struct ProxyState {
    pub identity: ProxyIdentity,
    pub stdb: Arc<MythicStdbClient>,
    pub registry: RegistryView,
    pub status: Arc<RwLock<ServerStatus>>,
}

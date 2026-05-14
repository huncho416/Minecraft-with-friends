

use mythiccord_plugin_routing::registry_view::RegistryView;
use mythiccord_plugin_routing::ProxyIdentity;
use mythiccord_stdb_bridge::{MythicStdbClient, ServerStatus};
use parking_lot::RwLock;
use std::sync::Arc;

pub struct ProxyState {
    pub identity: ProxyIdentity,

    #[allow(dead_code)]
    pub stdb: Arc<MythicStdbClient>,
    pub registry: RegistryView,
    pub status: Arc<RwLock<ServerStatus>>,
}

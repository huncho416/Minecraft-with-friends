

#![allow(clippy::doc_markdown)]
#![allow(clippy::module_name_repetitions)]

pub mod heartbeat;
pub mod registry_view;
pub mod router;

use mythiccord_stdb_bridge::{MythicStdbClient, ServerRole, ServerStatus};
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct ProxyIdentity {

    pub shard_id: String,
    pub role: ServerRole,
    pub region: String,

    pub address: String,
    pub max_players: u32,
}

#[derive(Clone)]
pub struct RoutingRuntime {
    pub identity: ProxyIdentity,
    pub stdb: Arc<MythicStdbClient>,

    pub registry: registry_view::RegistryView,

    pub status: Arc<parking_lot::RwLock<ServerStatus>>,
}

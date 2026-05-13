//! Local mirror of the `server_registry` table.
//!
//! Subscribes once at boot, applies row events as they stream in, and
//! exposes a cheap-to-clone snapshot the router can read without any
//! network round-trip.

use mythiccord_stdb_bridge::handle::{TableEvent, TableOp};
use mythiccord_stdb_bridge::schema::table;
use mythiccord_stdb_bridge::StdbHandle;
use parking_lot::RwLock;
use serde::Deserialize;
use std::collections::HashMap;
use std::sync::Arc;
use tracing::{debug, warn};

/// Mirror of `mythic_stdb::registry::ServerEntry`. Field names match the
/// Rust struct exactly so serde_json deserializes without a custom impl.
#[derive(Debug, Clone, Deserialize)]
pub struct ServerEntry {
    pub shard_id: String,
    pub role: String,
    pub region: String,
    pub status: String,
    pub address: String,
    pub max_players: u32,
    pub player_count: u32,
    pub tps: f32,
    pub heap_load: f32,
    pub schema_version: u32,
    pub started_at: i64,
    pub last_heartbeat: i64,
}

#[derive(Clone, Default)]
pub struct RegistryView {
    inner: Arc<RwLock<HashMap<String, ServerEntry>>>,
}

impl RegistryView {
    pub fn new() -> Self {
        Self::default()
    }

    /// Snapshot — cheap clone of the current map. Use this as the input
    /// to [`crate::router::pick_shard`].
    pub fn snapshot(&self) -> Vec<ServerEntry> {
        self.inner.read().values().cloned().collect()
    }

    pub fn get(&self, shard_id: &str) -> Option<ServerEntry> {
        self.inner.read().get(shard_id).cloned()
    }

    pub fn len(&self) -> usize {
        self.inner.read().len()
    }

    pub fn is_empty(&self) -> bool {
        self.inner.read().is_empty()
    }

    fn apply(&self, event: &TableEvent) {
        match event.op {
            TableOp::Insert | TableOp::Update => {
                match serde_json::from_str::<ServerEntry>(&event.payload) {
                    Ok(entry) => {
                        self.inner.write().insert(entry.shard_id.clone(), entry);
                    }
                    Err(e) => warn!("registry_view: bad row {}: {e}", event.payload),
                }
            }
            TableOp::Delete => {
                // Delete payloads carry only the PK fields in STDB.
                if let Ok(stub) = serde_json::from_str::<serde_json::Value>(&event.payload)
                    && let Some(id) = stub.get("shard_id").and_then(|v| v.as_str())
                {
                    self.inner.write().remove(id);
                }
            }
        }
    }
}

/// Spawn the subscription task. Drops the subscription if the bridge
/// disconnects — the bridge's own reconnect loop re-subscribes for us.
pub fn spawn(handle: StdbHandle, view: RegistryView) -> tokio::task::JoinHandle<()> {
    tokio::spawn(async move {
        let mut events = match handle.subscribe(table::SERVER_REGISTRY).await {
            Ok(rx) => rx,
            Err(e) => {
                warn!("registry_view subscribe failed: {e}");
                return;
            }
        };
        debug!("registry_view: subscribed");
        while let Some(event) = events.recv().await {
            view.apply(&event);
        }
        debug!("registry_view: stream ended");
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use mythiccord_stdb_bridge::handle::{TableEvent, TableOp};

    fn entry_payload(shard: &str, status: &str, players: u32) -> String {
        format!(
            r#"{{
                "shard_id": "{shard}",
                "role": "SKYBLOCK",
                "region": "us-east",
                "status": "{status}",
                "address": "{shard}:25565",
                "max_players": 100,
                "player_count": {players},
                "tps": 19.5,
                "heap_load": 0.4,
                "schema_version": 1,
                "started_at": 1000,
                "last_heartbeat": 2000
            }}"#
        )
    }

    #[test]
    fn insert_then_update_then_delete() {
        let view = RegistryView::new();
        view.apply(&TableEvent {
            table: table::SERVER_REGISTRY,
            op: TableOp::Insert,
            payload: entry_payload("sb-1", "HEALTHY", 10),
        });
        assert_eq!(view.len(), 1);
        assert_eq!(view.get("sb-1").unwrap().player_count, 10);

        view.apply(&TableEvent {
            table: table::SERVER_REGISTRY,
            op: TableOp::Update,
            payload: entry_payload("sb-1", "HEALTHY", 20),
        });
        assert_eq!(view.get("sb-1").unwrap().player_count, 20);

        view.apply(&TableEvent {
            table: table::SERVER_REGISTRY,
            op: TableOp::Delete,
            payload: r#"{"shard_id":"sb-1"}"#.into(),
        });
        assert!(view.is_empty());
    }
}

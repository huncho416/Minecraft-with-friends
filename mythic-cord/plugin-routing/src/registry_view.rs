

use mythiccord_stdb_bridge::handle::{TableEvent, TableOp};
use mythiccord_stdb_bridge::schema::table;
use mythiccord_stdb_bridge::StdbHandle;
use parking_lot::RwLock;
use serde::Deserialize;
use std::collections::HashMap;
use std::sync::Arc;
use tracing::{debug, warn};

use serde::Deserializer;

fn deserialize_stdb_timestamp<'de, D>(deserializer: D) -> Result<i64, D::Error>
where
    D: Deserializer<'de>,
{
    #[derive(Deserialize)]
    #[serde(untagged)]
    enum TimestampRaw {
        Int(i64),
        Map { __timestamp_micros_since_unix_epoch__: i64 },
    }

    match TimestampRaw::deserialize(deserializer)? {
        TimestampRaw::Int(i) => Ok(i),
        TimestampRaw::Map { __timestamp_micros_since_unix_epoch__ } => Ok(__timestamp_micros_since_unix_epoch__),
    }
}

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
    #[serde(deserialize_with = "deserialize_stdb_timestamp")]
    pub started_at: i64,
    #[serde(deserialize_with = "deserialize_stdb_timestamp")]
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

    pub fn insert_entry(&self, entry: ServerEntry) {
        self.inner.write().insert(entry.shard_id.clone(), entry);
    }

    pub fn remove_entry(&self, shard_id: &str) {
        self.inner.write().remove(shard_id);
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

                if let Ok(stub) = serde_json::from_str::<serde_json::Value>(&event.payload)
                    && let Some(id) = stub.get("shard_id").and_then(|v| v.as_str())
                {
                    self.inner.write().remove(id);
                }
            }
        }
    }
}

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

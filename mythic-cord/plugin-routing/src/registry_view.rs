

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
        Arr(Vec<i64>),
        Map { __timestamp_micros_since_unix_epoch__: i64 },
    }

    match TimestampRaw::deserialize(deserializer)? {
        TimestampRaw::Int(i) => Ok(i),
        TimestampRaw::Arr(v) => Ok(v.into_iter().next().unwrap_or(0)),
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

/// Polls the STDB HTTP SQL endpoint every 5 seconds and refreshes the view.
/// Runs alongside the WebSocket subscription as a safety net: the WS
/// initial-state replay sometimes silently drops rows on this build of
/// SpacetimeDB, so we re-establish ground truth from SQL on every tick.
pub fn spawn_http_poll(stdb_http_url: String, view: RegistryView) -> tokio::task::JoinHandle<()> {
    tokio::spawn(async move {
        let client = match reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(3))
            .build()
        {
            Ok(c) => c,
            Err(e) => {
                warn!("registry_view http poll: client build failed: {e}");
                return;
            }
        };
        let endpoint = format!("{}/v1/database/mythicpvp/sql", stdb_http_url.trim_end_matches('/'));
        let mut tick = tokio::time::interval(std::time::Duration::from_secs(5));
        loop {
            tick.tick().await;
            match client.post(&endpoint)
                .header("Content-Type", "text/plain")
                .body("SELECT * FROM server_registry")
                .send()
                .await
            {
                Ok(resp) => match resp.text().await {
                    Ok(body) => apply_sql_snapshot(&body, &view),
                    Err(e) => debug!("registry_view http poll: body read failed: {e}"),
                },
                Err(e) => debug!("registry_view http poll: request failed: {e}"),
            }
        }
    })
}

fn apply_sql_snapshot(body: &str, view: &RegistryView) {
    let root = match serde_json::from_str::<serde_json::Value>(body) {
        Ok(v) => v,
        Err(e) => {
            debug!("registry_view http poll: parse failed: {e}");
            return;
        }
    };
    let table = match root.get(0).and_then(|t| t.get("rows")).and_then(|r| r.as_array()) {
        Some(r) => r,
        None => return,
    };
    let columns = match root.get(0).and_then(|t| t.get("schema")).and_then(|s| s.get("elements")).and_then(|e| e.as_array()) {
        Some(c) => c,
        None => return,
    };
    let mut seen = std::collections::HashSet::new();
    for row_val in table {
        let row_arr = match row_val.as_array() {
            Some(a) => a,
            None => continue,
        };
        let mut obj = serde_json::Map::new();
        for (i, col) in columns.iter().enumerate() {
            let name = col.get("name").and_then(|n| n.get("some")).and_then(|s| s.as_str());
            if let (Some(name), Some(value)) = (name, row_arr.get(i)) {
                obj.insert(name.to_string(), value.clone());
            }
        }
        let payload = serde_json::Value::Object(obj).to_string();
        match serde_json::from_str::<ServerEntry>(&payload) {
            Ok(entry) => {
                seen.insert(entry.shard_id.clone());
                if entry.status.eq_ignore_ascii_case("HEALTHY") {
                    view.insert_entry(entry);
                } else {
                    view.remove_entry(&entry.shard_id);
                }
            }
            Err(e) => debug!("registry_view http poll: row decode failed: {e}"),
        }
    }
    let known: Vec<String> = view.snapshot().into_iter().map(|e| e.shard_id).collect();
    for id in known {
        if !seen.contains(&id) {
            view.remove_entry(&id);
        }
    }
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

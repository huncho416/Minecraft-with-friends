#![cfg(feature = "with-infrarust")]

use crate::config::Config;
use mythiccord_plugin_routing::registry_view::{RegistryView, ServerEntry};
use mythiccord_stdb_bridge::ServerStatus;
use serde::Serialize;
use std::collections::HashSet;
use std::path::PathBuf;
use std::time::Duration;
use tokio::fs;
use tracing::{debug, info, warn};

#[derive(Debug, Serialize)]
struct ExportedServerConfig<'a> {
    name: &'a str,
    #[serde(skip_serializing_if = "Option::is_none")]
    network: Option<String>,
    addresses: Vec<&'a str>,
    domains: Vec<String>,
    proxy_mode: &'static str,
    max_players: u32,
    send_proxy_protocol: bool,
}

#[derive(Clone)]
pub struct ConfigExporter {
    registry: RegistryView,
    servers_dir: PathBuf,
    domain_suffix: String,
    debounce: Duration,
    proxy_mode: String,
    send_proxy_protocol: bool,
    proxy_shard_id: String,
}

impl ConfigExporter {
    pub fn from_config(registry: RegistryView, cfg: &Config) -> Self {
        let exp = &cfg.config_export;
        Self {
            registry,
            servers_dir: PathBuf::from(&exp.servers_dir),
            domain_suffix: exp.domain_suffix.clone(),
            debounce: Duration::from_millis(exp.debounce_ms),
            proxy_mode: exp.proxy_mode.clone(),
            send_proxy_protocol: exp.send_proxy_protocol,
            proxy_shard_id: cfg.identity.shard_id.clone(),
        }
    }

    pub async fn run(self) {
        if let Err(e) = fs::create_dir_all(&self.servers_dir).await {
            warn!(?e, dir = %self.servers_dir.display(), "failed to create servers_dir");
            return;
        }
        info!(
            dir = %self.servers_dir.display(),
            "config exporter running"
        );
        let mut last_snapshot = String::new();
        loop {
            let entries = self.registry.snapshot();
            let snapshot_key = snapshot_fingerprint(&entries);
            if snapshot_key != last_snapshot {
                if let Err(e) = self.write_all(&entries).await {
                    warn!(?e, "config export write failed");
                } else {
                    last_snapshot = snapshot_key;
                }
            }
            tokio::time::sleep(self.debounce).await;
        }
    }

    async fn write_all(&self, entries: &[ServerEntry]) -> std::io::Result<()> {
        let mut keep: HashSet<String> = HashSet::new();
        for entry in entries {
            if entry.status != ServerStatus::Healthy.wire() {
                continue;
            }
            if entry.shard_id == self.proxy_shard_id {
                continue;
            }
            keep.insert(entry.shard_id.clone());
            self.write_one(entry).await?;
        }
        self.prune_stale(&keep).await
    }

    async fn write_one(&self, entry: &ServerEntry) -> std::io::Result<()> {
        let proxy_mode = proxy_mode_str(&self.proxy_mode);
        let network = if proxy_mode_supports_network(proxy_mode) {
            Some(sanitize_network(&entry.role))
        } else {
            None
        };
        let cfg = ExportedServerConfig {
            name: &entry.shard_id,
            network,
            addresses: vec![entry.address.as_str()],
            domains: self.domains_for(entry),
            proxy_mode,
            max_players: entry.max_players,
            send_proxy_protocol: self.send_proxy_protocol,
        };
        let body = toml::to_string_pretty(&cfg)
            .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e.to_string()))?;
        let final_path = self.servers_dir.join(format!("{}.toml", entry.shard_id));
        let tmp_path = final_path.with_extension("toml.tmp");
        fs::write(&tmp_path, body.as_bytes()).await?;
        fs::rename(&tmp_path, &final_path).await?;
        debug!(shard = %entry.shard_id, "exported server config");
        Ok(())
    }

    async fn prune_stale(&self, keep: &HashSet<String>) -> std::io::Result<()> {
        let mut dir = fs::read_dir(&self.servers_dir).await?;
        while let Some(entry) = dir.next_entry().await? {
            let path = entry.path();
            if path.extension().and_then(|s| s.to_str()) != Some("toml") {
                continue;
            }
            let stem = match path.file_stem().and_then(|s| s.to_str()) {
                Some(s) => s.to_string(),
                None => continue,
            };
            if !keep.contains(&stem) {
                if let Err(e) = fs::remove_file(&path).await {
                    warn!(?e, file = %path.display(), "failed to remove stale server config");
                } else {
                    debug!(shard = %stem, "removed stale server config");
                }
            }
        }
        Ok(())
    }

    fn domains_for(&self, entry: &ServerEntry) -> Vec<String> {
        let suffix = self.domain_suffix.trim().trim_end_matches('.');
        if suffix.is_empty() {
            return vec![entry.shard_id.clone()];
        }
        let mut domains = vec![
            entry.shard_id.clone(),
            format!("{}.{}", entry.shard_id, suffix),
        ];
        if entry.role.eq_ignore_ascii_case("HUB") {
            domains.insert(0, suffix.to_string());
        }
        domains
    }
}

fn snapshot_fingerprint(entries: &[ServerEntry]) -> String {
    let mut keys: Vec<String> = entries
        .iter()
        .filter(|e| e.status == ServerStatus::Healthy.wire())
        .map(|e| {
            format!(
                "{}|{}|{}|{}|{}",
                e.shard_id, e.role, e.address, e.max_players, e.region
            )
        })
        .collect();
    keys.sort();
    keys.join("\n")
}

fn proxy_mode_str(input: &str) -> &'static str {
    match input.to_ascii_lowercase().as_str() {
        "passthrough" => "passthrough",
        "zerocopy" | "zero_copy" | "zero-copy" => "zero_copy",
        "clientonly" | "client_only" | "client-only" => "client_only",
        "offline" => "offline",
        "serveronly" | "server_only" | "server-only" => "server_only",
        _ => "passthrough",
    }
}

fn proxy_mode_supports_network(proxy_mode: &str) -> bool {
    matches!(proxy_mode, "client_only" | "offline")
}

fn sanitize_network(role: &str) -> String {
    role.chars()
        .map(|c| if c.is_ascii_alphanumeric() { c.to_ascii_lowercase() } else if c == '_' || c == '-' { c } else { '_' })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn entry(shard: &str, role: &str, status: ServerStatus, players: u32) -> ServerEntry {
        ServerEntry {
            shard_id: shard.into(),
            role: role.into(),
            region: "us-east".into(),
            status: status.wire().into(),
            address: format!("{shard}.svc.local:25565"),
            max_players: 100,
            player_count: players,
            tps: 19.5,
            heap_load: 0.4,
            schema_version: 1,
            started_at: 1000,
            last_heartbeat: 2000,
        }
    }

    fn exporter(view: RegistryView, dir: PathBuf) -> ConfigExporter {
        ConfigExporter {
            registry: view,
            servers_dir: dir,
            domain_suffix: "mythic.test".into(),
            debounce: Duration::from_millis(10),
            proxy_mode: "passthrough".into(),
            send_proxy_protocol: false,
            proxy_shard_id: "proxy-1".into(),
        }
    }

    #[tokio::test]
    async fn writes_one_toml_per_healthy_server_and_skips_offline() {
        let tmp = tempfile::tempdir().unwrap();
        let view = RegistryView::new();
        view.insert_entry(entry("hub-1", "HUB", ServerStatus::Healthy, 5));
        view.insert_entry(entry("sb-1", "SKYBLOCK", ServerStatus::Healthy, 30));
        view.insert_entry(entry("sb-2", "SKYBLOCK", ServerStatus::Offline, 0));
        let exp = exporter(view.clone(), tmp.path().to_path_buf());
        exp.write_all(&view.snapshot()).await.unwrap();
        let mut listing: Vec<String> = std::fs::read_dir(tmp.path())
            .unwrap()
            .filter_map(|e| e.ok())
            .filter_map(|e| e.file_name().to_str().map(str::to_string))
            .collect();
        listing.sort();
        assert_eq!(listing, vec!["hub-1.toml", "sb-1.toml"]);
        let body = std::fs::read_to_string(tmp.path().join("hub-1.toml")).unwrap();
        assert!(body.contains("name = \"hub-1\""));
        assert!(!body.contains("network = "));
        assert!(body.contains("addresses = [\"hub-1.svc.local:25565\"]"));
        assert!(body.contains("\"mythic.test\""));
        assert!(body.contains("\"hub-1\""));
        assert!(body.contains("\"hub-1.mythic.test\""));
        let skyblock_body = std::fs::read_to_string(tmp.path().join("sb-1.toml")).unwrap();
        assert!(!skyblock_body.contains("\"mythic.test\""));
        assert!(skyblock_body.contains("\"sb-1\""));
        assert!(skyblock_body.contains("\"sb-1.mythic.test\""));
    }

    #[tokio::test]
    async fn prune_removes_stale_files() {
        let tmp = tempfile::tempdir().unwrap();
        std::fs::write(tmp.path().join("ghost.toml"), "name = 'ghost'\n").unwrap();
        let view = RegistryView::new();
        view.insert_entry(entry("hub-1", "HUB", ServerStatus::Healthy, 5));
        let exp = exporter(view.clone(), tmp.path().to_path_buf());
        exp.write_all(&view.snapshot()).await.unwrap();
        let listing: Vec<String> = std::fs::read_dir(tmp.path())
            .unwrap()
            .filter_map(|e| e.ok())
            .filter_map(|e| e.file_name().to_str().map(str::to_string))
            .collect();
        assert!(listing.contains(&"hub-1.toml".to_string()));
        assert!(!listing.contains(&"ghost.toml".to_string()));
    }

    #[tokio::test]
    async fn client_only_proxy_mode_writes_lowercase_network() {
        let tmp = tempfile::tempdir().unwrap();
        let view = RegistryView::new();
        view.insert_entry(entry("hub-1", "HUB", ServerStatus::Healthy, 5));
        view.insert_entry(entry("sb-1", "SKYBLOCK", ServerStatus::Healthy, 0));
        let mut exp = exporter(view.clone(), tmp.path().to_path_buf());
        exp.proxy_mode = "client_only".into();
        exp.write_all(&view.snapshot()).await.unwrap();
        let hub_body = std::fs::read_to_string(tmp.path().join("hub-1.toml")).unwrap();
        let sky_body = std::fs::read_to_string(tmp.path().join("sb-1.toml")).unwrap();
        assert!(hub_body.contains("network = \"hub\""), "hub body: {hub_body}");
        assert!(sky_body.contains("network = \"skyblock\""), "sky body: {sky_body}");
        assert!(hub_body.contains("proxy_mode = \"client_only\""));
    }

    #[test]
    fn sanitize_network_lowercases_and_replaces_invalid_chars() {
        assert_eq!(sanitize_network("HUB"), "hub");
        assert_eq!(sanitize_network("SKYBLOCK"), "skyblock");
        assert_eq!(sanitize_network("Game.Mode"), "game_mode");
        assert_eq!(sanitize_network("evt-1"), "evt-1");
        assert_eq!(sanitize_network("under_score"), "under_score");
    }

    #[test]
    fn fingerprint_is_stable_across_snapshots() {
        let view = RegistryView::new();
        view.insert_entry(entry("hub-1", "HUB", ServerStatus::Healthy, 5));
        view.insert_entry(entry("sb-1", "SKYBLOCK", ServerStatus::Healthy, 30));
        let a = snapshot_fingerprint(&view.snapshot());
        let b = snapshot_fingerprint(&view.snapshot());
        assert_eq!(a, b);
    }
}

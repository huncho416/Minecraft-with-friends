//! Layered config: defaults < TOML file < env overrides.
//!
//! Pterodactyl drives the proxy via env vars (egg variables become
//! container env), so env wins over the file. The file is for ops who
//! want a long-lived shape under source control.

use mythiccord_stdb_bridge::ServerRole;
use serde::Deserialize;
use std::path::Path;

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct Config {
    pub stdb: StdbConfig,
    pub identity: IdentityConfig,
    pub admin: AdminConfig,
    pub log: LogConfig,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct StdbConfig {
    pub uri: String,
    pub module: String,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct IdentityConfig {
    pub shard_id: String,
    pub role: String,
    pub region: String,
    /// What backends would dial to reach us. Defaults to the bind addr.
    pub address: String,
    pub max_players: u32,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct AdminConfig {
    /// `host:port` for the admin/health/metrics HTTP surface.
    pub bind: String,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct LogConfig {
    /// `json` or `pretty`. Pterodactyl is happier with `json` so it can
    /// route stdout into a parseable log stream.
    pub format: String,
    /// `EnvFilter` directive — see `tracing-subscriber` docs.
    pub filter: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            stdb: StdbConfig {
                uri: "http://spacetimedb:3000".into(),
                module: "mythicpvp".into(),
            },
            identity: IdentityConfig {
                shard_id: "proxy-1".into(),
                role: "HUB".into(),
                region: "us-east".into(),
                address: "mythiccord:25565".into(),
                max_players: 500,
            },
            admin: AdminConfig {
                bind: "0.0.0.0:8080".into(),
            },
            log: LogConfig {
                format: "json".into(),
                filter: "info,mythiccord=debug".into(),
            },
        }
    }
}

impl Default for StdbConfig {
    fn default() -> Self { Config::default().stdb }
}
impl Default for IdentityConfig {
    fn default() -> Self { Config::default().identity }
}
impl Default for AdminConfig {
    fn default() -> Self { Config::default().admin }
}
impl Default for LogConfig {
    fn default() -> Self { Config::default().log }
}

impl Config {
    pub fn load(path: Option<&Path>) -> anyhow::Result<Self> {
        let mut cfg = match path {
            Some(p) if p.exists() => {
                let raw = std::fs::read_to_string(p)?;
                toml::from_str(&raw)?
            }
            _ => Self::default(),
        };
        cfg.apply_env();
        Ok(cfg)
    }

    fn apply_env(&mut self) {
        // Pterodactyl-egg-friendly env names. Keep these stable — the egg
        // JSON references them by exact key.
        if let Ok(v) = std::env::var("STDB_URI") { self.stdb.uri = v; }
        if let Ok(v) = std::env::var("STDB_MODULE") { self.stdb.module = v; }
        if let Ok(v) = std::env::var("MYTHIC_SHARD_ID") { self.identity.shard_id = v; }
        if let Ok(v) = std::env::var("MYTHIC_ROLE") { self.identity.role = v; }
        if let Ok(v) = std::env::var("MYTHIC_REGION") { self.identity.region = v; }
        if let Ok(v) = std::env::var("MYTHIC_ADDRESS") { self.identity.address = v; }
        if let Ok(v) = std::env::var("MYTHIC_MAX_PLAYERS")
            && let Ok(n) = v.parse()
        {
            self.identity.max_players = n;
        }
        if let Ok(v) = std::env::var("MYTHIC_ADMIN_BIND") { self.admin.bind = v; }
        if let Ok(v) = std::env::var("MYTHIC_LOG_FORMAT") { self.log.format = v; }
        if let Ok(v) = std::env::var("RUST_LOG") { self.log.filter = v; }
    }

    pub fn role(&self) -> anyhow::Result<ServerRole> {
        match self.identity.role.as_str() {
            "HUB" => Ok(ServerRole::Hub),
            "SKYBLOCK" => Ok(ServerRole::Skyblock),
            "PVP" => Ok(ServerRole::Pvp),
            "EVENT" => Ok(ServerRole::Event),
            other => anyhow::bail!("unknown role: {other} (want HUB|SKYBLOCK|PVP|EVENT)"),
        }
    }
}

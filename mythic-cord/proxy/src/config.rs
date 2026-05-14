

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
    pub config_export: ConfigExportConfig,
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

    pub address: String,
    pub max_players: u32,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct AdminConfig {

    pub bind: String,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct LogConfig {

    pub format: String,

    pub filter: String,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(default)]
pub struct ConfigExportConfig {
    pub servers_dir: String,
    pub domain_suffix: String,
    pub debounce_ms: u64,
    pub proxy_mode: String,
    pub send_proxy_protocol: bool,
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
            config_export: ConfigExportConfig {
                servers_dir: "/etc/infrarust/servers".into(),
                domain_suffix: "mythicpvp.local".into(),
                debounce_ms: 1000,
                proxy_mode: "passthrough".into(),
                send_proxy_protocol: false,
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
impl Default for ConfigExportConfig {
    fn default() -> Self { Config::default().config_export }
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
        if let Ok(v) = std::env::var("MYTHIC_CONFIG_EXPORT_DIR") { self.config_export.servers_dir = v; }
        if let Ok(v) = std::env::var("MYTHIC_CONFIG_DOMAIN_SUFFIX") { self.config_export.domain_suffix = v; }
        if let Ok(v) = std::env::var("MYTHIC_CONFIG_PROXY_MODE") { self.config_export.proxy_mode = v; }
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

//! Constants and enums mirroring `mythic-stdb`'s `common.rs`.
//!
//! Kept in sync by hand. When you change a constant on the Rust side of
//! the schema, change it here too in the same commit. The
//! [`SCHEMA_VERSION`] check at boot will catch the case where you forgot
//! and shipped anyway.

/// Must equal `mythic_stdb::SCHEMA_VERSION`.
pub const SCHEMA_VERSION: u32 = 1;

pub mod table {
    pub const MODULE_META: &str = "module_meta";
    pub const PLAYERS: &str = "players";
    pub const SERVER_REGISTRY: &str = "server_registry";
    pub const SESSIONS: &str = "sessions";
    pub const PUNISHMENTS: &str = "punishments";
}

pub mod reducer {
    // sessions
    pub const SESSION_LOGIN: &str = "session_login";
    pub const SESSION_LOGOUT: &str = "session_logout";
    pub const SESSION_ROUTE: &str = "session_route";
    pub const SESSION_TOUCH: &str = "session_touch";
    pub const SESSION_REAP: &str = "session_reap";

    // registry
    pub const REGISTRY_ANNOUNCE: &str = "registry_announce";
    pub const REGISTRY_HEARTBEAT: &str = "registry_heartbeat";
    pub const REGISTRY_DRAIN: &str = "registry_drain";

    // punishments — proxy reads `has_active` server-side (no reducer call)
    // but exposes pardons/issues for the admin API
    pub const PUNISH_ISSUE: &str = "punish_issue";
    pub const PUNISH_PARDON: &str = "punish_pardon";
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ServerRole {
    Hub,
    Skyblock,
    Pvp,
    Event,
}

impl ServerRole {
    pub const fn wire(self) -> &'static str {
        match self {
            Self::Hub => "HUB",
            Self::Skyblock => "SKYBLOCK",
            Self::Pvp => "PVP",
            Self::Event => "EVENT",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ServerStatus {
    Starting,
    Healthy,
    Degraded,
    Draining,
    Offline,
}

impl ServerStatus {
    pub const fn wire(self) -> &'static str {
        match self {
            Self::Starting => "STARTING",
            Self::Healthy => "HEALTHY",
            Self::Degraded => "DEGRADED",
            Self::Draining => "DRAINING",
            Self::Offline => "OFFLINE",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PunishmentKind {
    Warn,
    Mute,
    Kick,
    TempBan,
    PermaBan,
}

impl PunishmentKind {
    pub const fn wire(self) -> &'static str {
        match self {
            Self::Warn => "WARN",
            Self::Mute => "MUTE",
            Self::Kick => "KICK",
            Self::TempBan => "TEMP_BAN",
            Self::PermaBan => "PERMA_BAN",
        }
    }
}

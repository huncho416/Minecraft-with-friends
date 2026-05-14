//! Constants and enums mirroring `mythic-stdb`'s `common.rs`.
//!
//! Kept in sync by hand. When you change a constant on the Rust side of
//! the schema, change it here too in the same commit. The
//! [`SCHEMA_VERSION`] check at boot will catch the case where you forgot
//! and shipped anyway.

/// Must equal `mythic_stdb::SCHEMA_VERSION`.
pub const SCHEMA_VERSION: u32 = 2;

pub mod table {
    pub const MODULE_META: &str = "module_meta";
    pub const PLAYERS: &str = "players";
    pub const SERVER_REGISTRY: &str = "server_registry";
    pub const SESSIONS: &str = "sessions";
    pub const PUNISHMENTS: &str = "punishments";
    pub const PUNISHMENT_TEMPLATES: &str = "punishment_templates";
    pub const PUNISHMENT_BLACKLIST: &str = "punishment_blacklist";
    pub const RANK_DEFINITIONS: &str = "rank_definitions";
    pub const RANK_GRANTS: &str = "rank_grants";
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

    // punishments
    pub const PUNISH_ISSUE: &str = "punish_issue";
    pub const PUNISH_PARDON: &str = "punish_pardon";
    pub const PUNISH_CLEAR_HISTORY: &str = "punish_clear_history";

    // templates / blacklist
    pub const TEMPLATE_UPSERT: &str = "template_upsert";
    pub const TEMPLATE_REMOVE: &str = "template_remove";
    pub const BLACKLIST_ADD: &str = "blacklist_add";
    pub const BLACKLIST_REVOKE: &str = "blacklist_revoke";

    // ranks
    pub const RANK_DEFINE: &str = "rank_define";
    pub const RANK_REMOVE: &str = "rank_remove";
    pub const GRANT_ISSUE: &str = "grant_issue";
    pub const GRANT_DEACTIVATE: &str = "grant_deactivate";
    pub const GRANT_REMOVE_INACTIVE: &str = "grant_remove_inactive";
    pub const GRANT_CLEAR: &str = "grant_clear";
    pub const GRANT_EXPIRE: &str = "grant_expire";
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
    TempMute,
    Kick,
    Ban,
    TempBan,
    Blacklist,
}

impl PunishmentKind {
    pub const fn wire(self) -> &'static str {
        match self {
            Self::Warn => "WARN",
            Self::Mute => "MUTE",
            Self::TempMute => "TEMP_MUTE",
            Self::Kick => "KICK",
            Self::Ban => "BAN",
            Self::TempBan => "TEMP_BAN",
            Self::Blacklist => "BLACKLIST",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PunishmentCategory {
    Warn,
    Mute,
    Ban,
    Blacklist,
}

impl PunishmentCategory {
    pub const fn wire(self) -> &'static str {
        match self {
            Self::Warn => "WARN",
            Self::Mute => "MUTE",
            Self::Ban => "BAN",
            Self::Blacklist => "BLACKLIST",
        }
    }
}

/// Where a rank grant came from. Mirrors `common::grant_source`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GrantSource {
    Staff,
    Purchase,
    Promotion,
    System,
}

impl GrantSource {
    pub const fn wire(self) -> &'static str {
        match self {
            Self::Staff => "STAFF",
            Self::Purchase => "PURCHASE",
            Self::Promotion => "PROMOTION",
            Self::System => "SYSTEM",
        }
    }
}

//! Shared types and helpers used across schema modules.
//!
//! SpacetimeDB tables can only hold supported primitive types, so anything
//! that looks like a "domain enum" is represented here as a small wrapper
//! around a `String` or `u8` with constants. This keeps the wire schema
//! flat and forward-compatible.

use spacetimedb::ReducerContext;

/// Result alias for reducers. Errors bubble up to the calling client as a
/// rejected reducer with the message attached.
pub type ReducerResult<T = ()> = Result<T, String>;

/// Helper: reject a reducer with a formatted message.
#[macro_export]
macro_rules! reject {
    ($($arg:tt)*) => {
        return Err(format!($($arg)*))
    };
}

/// Player UUID newtype-ish wrapper used in signatures for clarity.
/// Stored as `String` (canonical 36-char hyphenated form).
pub type PlayerUuid = String;

/// Shard identifier (e.g. `hub-1`, `sb-3`). Free-form to allow region tags.
pub type ShardId = String;

/// Currencies tracked by [`crate::economy`]. Keep in lockstep with the Java
/// `Currency` enum in `suite-api`.
pub mod currency {
    pub const COINS: &str = "COINS";
    pub const POINTS: &str = "POINTS";
    pub const GEMS: &str = "GEMS";

    pub const ALL: &[&str] = &[COINS, POINTS, GEMS];

    pub fn is_valid(c: &str) -> bool {
        ALL.contains(&c)
    }
}

/// Punishment kinds. String-typed for forward-compat with new categories.
pub mod punishment_kind {
    pub const WARN: &str = "WARN";
    pub const MUTE: &str = "MUTE";
    pub const KICK: &str = "KICK";
    pub const TEMP_BAN: &str = "TEMP_BAN";
    pub const PERMA_BAN: &str = "PERMA_BAN";

    pub const ALL: &[&str] = &[WARN, MUTE, KICK, TEMP_BAN, PERMA_BAN];

    pub fn is_valid(k: &str) -> bool {
        ALL.contains(&k)
    }
}

/// Server-role tags for [`crate::registry`].
pub mod server_role {
    pub const HUB: &str = "HUB";
    pub const SKYBLOCK: &str = "SKYBLOCK";
    pub const PVP: &str = "PVP";
    pub const EVENT: &str = "EVENT";
}

/// Server health status.
pub mod server_status {
    pub const STARTING: &str = "STARTING";
    pub const HEALTHY: &str = "HEALTHY";
    pub const DEGRADED: &str = "DEGRADED";
    pub const DRAINING: &str = "DRAINING";
    pub const OFFLINE: &str = "OFFLINE";
}

/// Cosmetic categories. Mirrors the Java `CosmeticType` enum.
pub mod cosmetic_type {
    pub const HAT: &str = "HAT";
    pub const TITLE: &str = "TITLE";
    pub const PARTICLE: &str = "PARTICLE";
    pub const KILL_EFFECT: &str = "KILL_EFFECT";
    pub const WIN_EFFECT: &str = "WIN_EFFECT";
    pub const CHAT_TAG: &str = "CHAT_TAG";
}

/// Reject the call if `uuid` isn't a 36-char hyphenated UUID.
/// We don't do strict hex validation here — Mojang already does that.
pub fn require_uuid(uuid: &str) -> ReducerResult {
    if uuid.len() != 36 || uuid.as_bytes().iter().filter(|&&b| b == b'-').count() != 4 {
        return Err(format!("invalid uuid: {uuid:?}"));
    }
    Ok(())
}

/// Reject the call when the message isn't from a trusted backend.
///
/// In production, only the proxy and game servers should be able to call
/// state-mutating reducers; player clients subscribe but never write. We
/// gate this by checking the caller's `Identity` against a small admin set
/// stored in `module_meta` once the proxy provisions its credentials.
///
/// For now this is a no-op so the suite test harness can run unauthed; it
/// becomes strict once the proxy ships.
#[allow(unused_variables)]
pub fn require_backend(ctx: &ReducerContext) -> ReducerResult {
    // TODO(phase-2): check against an `admins` table once proxy creds exist.
    Ok(())
}

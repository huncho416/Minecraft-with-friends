

use spacetimedb::ReducerContext;

pub type ReducerResult<T = ()> = Result<T, String>;

#[macro_export]
macro_rules! reject {
    ($($arg:tt)*) => {
        return Err(format!($($arg)*))
    };
}

pub type PlayerUuid = String;

pub type ShardId = String;

pub mod currency {
    pub const COINS: &str = "COINS";
    pub const POINTS: &str = "POINTS";
    pub const GEMS: &str = "GEMS";

    pub const ALL: &[&str] = &[COINS, POINTS, GEMS];

    pub fn is_valid(c: &str) -> bool {
        ALL.contains(&c)
    }
}

pub mod punishment_kind {
    pub const WARN: &str = "WARN";
    pub const MUTE: &str = "MUTE";
    pub const TEMP_MUTE: &str = "TEMP_MUTE";
    pub const KICK: &str = "KICK";
    pub const BAN: &str = "BAN";
    pub const TEMP_BAN: &str = "TEMP_BAN";
    pub const BLACKLIST: &str = "BLACKLIST";

    pub const ALL: &[&str] = &[WARN, MUTE, TEMP_MUTE, KICK, BAN, TEMP_BAN, BLACKLIST];

    pub fn is_valid(k: &str) -> bool {
        ALL.contains(&k)
    }
}

pub mod punishment_category {
    pub const WARN: &str = "WARN";
    pub const MUTE: &str = "MUTE";
    pub const BAN: &str = "BAN";
    pub const BLACKLIST: &str = "BLACKLIST";

    pub const ALL: &[&str] = &[WARN, MUTE, BAN, BLACKLIST];

    pub fn is_valid(c: &str) -> bool {
        ALL.contains(&c)
    }
}

pub mod grant_source {
    pub const STAFF: &str = "STAFF";
    pub const PURCHASE: &str = "PURCHASE";
    pub const PROMOTION: &str = "PROMOTION";
    pub const SYSTEM: &str = "SYSTEM";
}

pub mod server_role {
    pub const HUB: &str = "HUB";
    pub const SKYBLOCK: &str = "SKYBLOCK";
    pub const PVP: &str = "PVP";
    pub const EVENT: &str = "EVENT";
}

pub mod server_status {
    pub const STARTING: &str = "STARTING";
    pub const HEALTHY: &str = "HEALTHY";
    pub const DEGRADED: &str = "DEGRADED";
    pub const DRAINING: &str = "DRAINING";
    pub const OFFLINE: &str = "OFFLINE";
}

pub mod cosmetic_type {
    pub const HAT: &str = "HAT";
    pub const TITLE: &str = "TITLE";
    pub const PARTICLE: &str = "PARTICLE";
    pub const KILL_EFFECT: &str = "KILL_EFFECT";
    pub const WIN_EFFECT: &str = "WIN_EFFECT";
    pub const CHAT_TAG: &str = "CHAT_TAG";
}

pub fn require_uuid(uuid: &str) -> ReducerResult {
    if uuid.len() != 36 || uuid.as_bytes().iter().filter(|&&b| b == b'-').count() != 4 {
        return Err(format!("invalid uuid: {uuid:?}"));
    }
    Ok(())
}

#[allow(unused_variables)]
pub fn require_backend(ctx: &ReducerContext) -> ReducerResult {

    Ok(())
}

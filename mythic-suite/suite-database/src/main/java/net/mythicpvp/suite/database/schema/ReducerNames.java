package net.mythicpvp.suite.database.schema;

/**
 * Reducer name constants — mirrors {@code #[reducer]} functions in
 * {@code mythic-cord/stdb/src/*.rs}. The typed wrappers in
 * {@link MythicSchema} are the preferred call path; these exist for
 * dynamic call sites (admin tools, debug commands).
 */
public final class ReducerNames {

    private ReducerNames() {}

    // players.rs
    public static final String PLAYER_SET_RANK = "player_set_rank";
    public static final String PLAYER_SET_REGION = "player_set_region";

    // registry.rs
    public static final String REGISTRY_ANNOUNCE = "registry_announce";
    public static final String REGISTRY_HEARTBEAT = "registry_heartbeat";
    public static final String REGISTRY_DRAIN = "registry_drain";

    // sessions.rs
    public static final String SESSION_LOGIN = "session_login";
    public static final String SESSION_LOGOUT = "session_logout";
    public static final String SESSION_ROUTE = "session_route";
    public static final String SESSION_TOUCH = "session_touch";
    public static final String SESSION_REAP = "session_reap";

    // punishments.rs
    public static final String PUNISH_ISSUE = "punish_issue";
    public static final String PUNISH_PARDON = "punish_pardon";
    public static final String PUNISH_EXPIRE = "punish_expire";
    public static final String APPEAL_OPEN = "appeal_open";
    public static final String APPEAL_REVIEW = "appeal_review";

    // economy.rs
    public static final String ECONOMY_ADJUST = "economy_adjust";
    public static final String ECONOMY_TRANSFER = "economy_transfer";
    public static final String ECONOMY_ROLLBACK = "economy_rollback";

    // cosmetics.rs
    public static final String COSMETIC_GRANT = "cosmetic_grant";
    public static final String COSMETIC_EQUIP = "cosmetic_equip";

    // social.rs
    public static final String FRIEND_REQUEST = "friend_request";
    public static final String FRIEND_ACCEPT = "friend_accept";
    public static final String FRIEND_REMOVE = "friend_remove";
    public static final String PARTY_CREATE = "party_create";
    public static final String PARTY_JOIN = "party_join";
    public static final String PARTY_LEAVE = "party_leave";
    public static final String PARTY_DISBAND = "party_disband";
    public static final String MAIL_SEND = "mail_send";
    public static final String MAIL_MARK_READ = "mail_mark_read";

    // gameplay.rs
    public static final String ISLAND_CREATE = "island_create";
    public static final String SKILL_GRANT_XP = "skill_grant_xp";
    public static final String STAT_INCREMENT = "stat_increment";
    public static final String STATS_RESET_DAILY = "stats_reset_daily";
    public static final String STATS_RESET_WEEKLY = "stats_reset_weekly";
    public static final String LEADERBOARD_REBUILD = "leaderboard_rebuild";
}

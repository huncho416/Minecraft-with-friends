package net.mythicpvp.suite.database.schema;

public final class ReducerNames {

    private ReducerNames() {}

    public static final String PLAYER_SET_RANK = "player_set_rank";
    public static final String PLAYER_SET_REGION = "player_set_region";

    public static final String REGISTRY_ANNOUNCE = "registry_announce";
    public static final String REGISTRY_HEARTBEAT = "registry_heartbeat";
    public static final String REGISTRY_DRAIN = "registry_drain";

    public static final String SESSION_LOGIN = "session_login";
    public static final String SESSION_LOGOUT = "session_logout";
    public static final String SESSION_ROUTE = "session_route";
    public static final String SESSION_TOUCH = "session_touch";
    public static final String SESSION_REAP = "session_reap";

    public static final String PUNISH_ISSUE = "punish_issue";
    public static final String PUNISH_PARDON = "punish_pardon";
    public static final String PUNISH_EXPIRE = "punish_expire";
    public static final String PUNISH_CLEAR_HISTORY = "punish_clear_history";
    public static final String TEMPLATE_UPSERT = "template_upsert";
    public static final String TEMPLATE_REMOVE = "template_remove";
    public static final String BLACKLIST_ADD = "blacklist_add";
    public static final String BLACKLIST_REVOKE = "blacklist_revoke";
    public static final String APPEAL_OPEN = "appeal_open";
    public static final String APPEAL_REVIEW = "appeal_review";

    public static final String RANK_DEFINE = "rank_define";
    public static final String RANK_REMOVE = "rank_remove";
    public static final String GRANT_ISSUE = "grant_issue";
    public static final String GRANT_DEACTIVATE = "grant_deactivate";
    public static final String GRANT_REMOVE_INACTIVE = "grant_remove_inactive";
    public static final String GRANT_CLEAR = "grant_clear";
    public static final String GRANT_EXPIRE = "grant_expire";

    public static final String ECONOMY_ADJUST = "economy_adjust";
    public static final String ECONOMY_TRANSFER = "economy_transfer";
    public static final String ECONOMY_ROLLBACK = "economy_rollback";

    public static final String COSMETIC_GRANT = "cosmetic_grant";
    public static final String COSMETIC_EQUIP = "cosmetic_equip";

    public static final String FRIEND_REQUEST = "friend_request";
    public static final String FRIEND_ACCEPT = "friend_accept";
    public static final String FRIEND_DENY = "friend_deny";
    public static final String FRIEND_REMOVE = "friend_remove";
    public static final String PARTY_CREATE = "party_create";
    public static final String PARTY_JOIN = "party_join";
    public static final String PARTY_LEAVE = "party_leave";
    public static final String PARTY_DISBAND = "party_disband";
    public static final String MAIL_SEND = "mail_send";
    public static final String MAIL_MARK_READ = "mail_mark_read";
    public static final String LOGIN_STREAK_RECORD = "login_streak_record";

    public static final String STAFF_CHAT_SEND = "staff_chat_send";
    public static final String STAFF_CHAT_PRUNE = "staff_chat_prune";
    public static final String TRANSFER_REQUEST_CREATE = "transfer_request_create";
    public static final String TRANSFER_REQUEST_COMPLETE = "transfer_request_complete";
    public static final String TRANSFER_REQUEST_PRUNE = "transfer_request_prune";
    public static final String REPORT_CREATE = "report_create";
    public static final String REPORT_RESOLVE = "report_resolve";

    public static final String ISLAND_CREATE = "island_create";
    public static final String SKILL_GRANT_XP = "skill_grant_xp";
    public static final String STAT_INCREMENT = "stat_increment";
    public static final String STATS_RESET_DAILY = "stats_reset_daily";
    public static final String STATS_RESET_WEEKLY = "stats_reset_weekly";
    public static final String LEADERBOARD_REBUILD = "leaderboard_rebuild";
}

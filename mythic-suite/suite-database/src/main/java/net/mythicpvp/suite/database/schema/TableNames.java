package net.mythicpvp.suite.database.schema;

/**
 * Table name constants — mirrors {@code #[table(name = …)]} declarations
 * in {@code mythic-cord/stdb/src/*.rs}. Use these instead of string
 * literals so renames are caught at compile time.
 */
public final class TableNames {

    private TableNames() {}

    public static final String MODULE_META = "module_meta";

    // players.rs
    public static final String PLAYERS = "players";

    // registry.rs
    public static final String SERVER_REGISTRY = "server_registry";

    // sessions.rs
    public static final String SESSIONS = "sessions";
    public static final String SESSION_HISTORY = "session_history";

    // punishments.rs
    public static final String PUNISHMENTS = "punishments";
    public static final String PUNISHMENT_APPEALS = "punishment_appeals";
    public static final String PUNISHMENT_TEMPLATES = "punishment_templates";
    public static final String PUNISHMENT_BLACKLIST = "punishment_blacklist";

    // ranks.rs
    public static final String RANK_DEFINITIONS = "rank_definitions";
    public static final String RANK_GRANTS = "rank_grants";

    // economy.rs
    public static final String TRANSACTIONS = "transactions";

    // cosmetics.rs
    public static final String COSMETIC_GRANTS = "cosmetic_grants";
    public static final String COSMETIC_EQUIPPED = "cosmetic_equipped";

    // social.rs
    public static final String FRIENDS = "friends";
    public static final String FRIEND_REQUESTS = "friend_requests";
    public static final String PARTIES = "parties";
    public static final String PARTY_MEMBERS = "party_members";
    public static final String MAIL = "mail";

    // gameplay.rs
    public static final String ISLANDS = "islands";
    public static final String ISLAND_MEMBERS = "island_members";
    public static final String SKILLS = "skills";
    public static final String STATS = "stats";
    public static final String LEADERBOARDS = "leaderboards";
}

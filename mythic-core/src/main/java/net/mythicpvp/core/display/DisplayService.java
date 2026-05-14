package net.mythicpvp.core.display;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.nametag.NametagManager;
import net.mythicpvp.suite.scoreboard.BoardManager;
import net.mythicpvp.suite.scoreboard.MythicBoard;
import net.mythicpvp.suite.tab.TabManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * Pushes per-player display state (tablist entry, nametag, scoreboard)
 * through the suite managers, sourcing values from {@link RankService}
 * and {@link GrantService} and resolving template placeholders against
 * {@link PlaceholderResolver}.
 *
 * <p>Threading: every public method must be called from the Bukkit
 * primary thread. The underlying NametagManager mutates per-viewer
 * scoreboard teams, which isn't safe off-main.
 *
 * <p>Failure mode: a player without an explicit grant falls back to the
 * {@code default} rank (matching {@link GrantService#activeRank}). A
 * server with no ranks at all falls back to a hard-coded grey
 * {@code Default} placeholder so we never NPE on join.
 */
public final class DisplayService {

    private static final String FALLBACK_RANK_ID = "default";

    private final JavaPlugin plugin;
    private final RankService rankService;
    private final GrantService grantService;
    private final String serverId;

    // Tab + scoreboard templates loaded from tablist.yml + scoreboard.yml.
    private List<String> tabHeader = List.of();
    private List<String> tabFooter = List.of();
    private String scoreboardTitle = "";
    private List<String> scoreboardLines = List.of();

    public DisplayService(
            @NotNull JavaPlugin plugin,
            @NotNull RankService rankService,
            @NotNull GrantService grantService,
            @NotNull String serverId) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.grantService = grantService;
        this.serverId = serverId;
    }

    /**
     * Read tablist + scoreboard templates from the supplied configs.
     * Called once at boot, again on {@code /reload}.
     */
    public void loadTemplates(@NotNull MythicConfig tablist, @NotNull MythicConfig scoreboard) {
        this.tabHeader = tablist.getStringList("tablist.header");
        this.tabFooter = tablist.getStringList("tablist.footer");
        // Pick the per-gamemode override whose key is a case-insensitive
        // prefix of the server id (e.g. "hub-1" matches the "hub" key).
        // Falls back to the top-level `scoreboard:` block.
        String overrideTitle = "";
        List<String> overrideLines = List.of();
        if (scoreboard.contains("gamemodes")) {
            org.bukkit.configuration.ConfigurationSection gm =
                    scoreboard.getConfig().getConfigurationSection("gamemodes");
            if (gm != null) {
                String lowerId = serverId.toLowerCase(java.util.Locale.ROOT);
                for (String key : gm.getKeys(false)) {
                    if (lowerId.startsWith(key.toLowerCase(java.util.Locale.ROOT))) {
                        overrideTitle = gm.getString(key + ".title", "");
                        overrideLines = gm.getStringList(key + ".lines");
                        break;
                    }
                }
            }
        }
        this.scoreboardTitle = overrideTitle.isEmpty()
                ? scoreboard.getString("scoreboard.title", "")
                : overrideTitle;
        this.scoreboardLines = overrideLines.isEmpty()
                ? scoreboard.getStringList("scoreboard.lines")
                : overrideLines;
    }

    /**
     * Push the full display state for one player. Called on join and
     * whenever their rank changes.
     */
    public void apply(@NotNull Player player) {
        CoreRank rank = activeRankOrFallback(player.getUniqueId());
        PlaceholderResolver ctx = contextFor(player, rank);

        applyTab(player, rank, ctx);
        applyNametag(player, rank, ctx);
        applyScoreboard(player, ctx);
    }

    /** Refresh display state for a player by uuid (post-grant, post-edit). */
    public void refresh(@NotNull UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            apply(player);
        }
    }

    /**
     * Refresh every online player. Use after a global change such as a
     * rank-edit that affects whoever holds it, or after the server's
     * online-player count changes (so {@code %online%} updates).
     */
    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    /** Drop tab entry / nametag / scoreboard state for a leaving player. */
    public void clear(@NotNull Player player) {
        TabManager.getInstance().remove(player);
        NametagManager.getInstance().remove(player);
        BoardManager.getInstance().remove(player);
    }

    // ── Internals ────────────────────────────────────────────────────

    @NotNull
    private CoreRank activeRankOrFallback(@NotNull UUID playerUuid) {
        String activeId = grantService.activeRank(playerUuid);
        CoreRank rank = rankService.get(activeId);
        if (rank != null) {
            return rank;
        }
        rank = rankService.get(FALLBACK_RANK_ID);
        return rank == null ? hardcodedFallback() : rank;
    }

    @NotNull
    private PlaceholderResolver contextFor(@NotNull Player player, @NotNull CoreRank rank) {
        return new PlaceholderResolver()
                .set("player", player.getName())
                .set("rank", rank.name())
                .set("rank_id", rank.id())
                .set("rank_color", rank.color())
                .set("server", serverId)
                .set("online", Integer.toString(onlineCount()))
                .set("chat_prefix", rank.chatPrefix())
                .set("tab_prefix", rank.tabPrefix())
                .set("nametag_prefix", rank.nametagPrefix())
                .set("prefix", rank.prefix())
                .set("suffix", rank.suffix());
    }

    private void applyTab(@NotNull Player player, @NotNull CoreRank rank, @NotNull PlaceholderResolver ctx) {
        TabManager tab = TabManager.getInstance();
        // Header / footer are joined per the YAML "list of lines" convention
        // — TabManager itself takes a single string per side, so we collapse
        // resolved lines with newlines. PAPI gets a second pass over the
        // joined string so its tokens (e.g. %vault_eco_balance%) resolve.
        String header = PapiBridge.apply(player, String.join("\n", ctx.resolveAll(tabHeader)));
        String footer = PapiBridge.apply(player, String.join("\n", ctx.resolveAll(tabFooter)));
        tab.setLayout(player, header, footer);
        tab.setEntry(player.getUniqueId(),
                PapiBridge.apply(player, ctx.resolve(rank.tabPrefix())),
                PapiBridge.apply(player, ctx.resolve(rank.suffix())),
                rank.weight());
        tab.apply(player);
    }

    private void applyNametag(@NotNull Player player, @NotNull CoreRank rank, @NotNull PlaceholderResolver ctx) {
        NametagManager.getInstance().setNametag(
                player,
                ctx.resolve(rank.nametagPrefix()),
                ctx.resolve(rank.suffix()),
                rank.weight(),
                null);
    }

    private void applyScoreboard(@NotNull Player player, @NotNull PlaceholderResolver ctx) {
        if (scoreboardTitle.isEmpty() && scoreboardLines.isEmpty()) {
            // No board configured — don't create one. Callers that want
            // a per-gamemode board can build it directly via BoardManager.
            return;
        }
        MythicBoard board = BoardManager.getInstance().get(player);
        String title = PapiBridge.apply(player, ctx.resolve(scoreboardTitle));
        if (board == null) {
            board = BoardManager.getInstance().create(player, title);
        } else {
            board.setTitle(title);
        }
        java.util.List<String> resolved = ctx.resolveAll(scoreboardLines);
        java.util.List<String> withPapi = new java.util.ArrayList<>(resolved.size());
        for (String line : resolved) {
            withPapi.add(PapiBridge.apply(player, line));
        }
        board.setLines(withPapi);
    }

    /**
     * Online-player count, computed at apply time so {@code %online%}
     * stays current. Pulled out as a seam for tests.
     */
    @NotNull
    IntSupplier onlineCounter() {
        return () -> Bukkit.getOnlinePlayers().size();
    }

    private int onlineCount() {
        try {
            return onlineCounter().getAsInt();
        } catch (Exception ignore) {
            return 0;
        }
    }

    @NotNull
    private static CoreRank hardcodedFallback() {
        return new CoreRank(
                FALLBACK_RANK_ID, "Default", "#808080",
                org.bukkit.Material.LIGHT_GRAY_DYE,
                "&7", "", 1000, false, false, "",
                List.of(),
                "&7", "%chat_prefix%%player%&7: &f%message%",
                "&7", "%tab_prefix%%player%",
                "&7", "%nametag_prefix%%player%");
    }

    /** For tests — direct access to the loaded tab header. */
    @NotNull
    List<String> tabHeader() { return tabHeader; }

    /** For tests — direct access to the loaded scoreboard lines. */
    @NotNull
    List<String> scoreboardLines() { return scoreboardLines; }

    /** For tests — direct access to the loaded scoreboard title. */
    @NotNull
    String scoreboardTitle() { return scoreboardTitle; }

    /** Plugin reference — used by the session listener for scheduled refreshes. */
    @NotNull
    public JavaPlugin plugin() { return plugin; }
}

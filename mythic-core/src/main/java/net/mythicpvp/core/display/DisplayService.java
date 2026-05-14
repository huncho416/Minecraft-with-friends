package net.mythicpvp.core.display;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.disguise.DisguiseManager;
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

public final class DisplayService {

    private static final String FALLBACK_RANK_ID = "default";

    private final JavaPlugin plugin;
    private final RankService rankService;
    private final GrantService grantService;
    private final String serverId;

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

    public void loadTemplates(@NotNull MythicConfig tablist, @NotNull MythicConfig scoreboard) {
        this.tabHeader = tablist.getStringList("tablist.header");
        this.tabFooter = tablist.getStringList("tablist.footer");

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

    public void apply(@NotNull Player player) {

        String rankOverride = DisguiseManager.getInstance().getRankOverride(player.getUniqueId());
        CoreRank rank = rankOverride != null
                ? rankOrFallback(rankOverride)
                : activeRankOrFallback(player.getUniqueId());
        PlaceholderResolver ctx = contextFor(player, rank);

        applyTab(player, rank, ctx);
        applyNametag(player, rank, ctx);
        applyScoreboard(player, ctx);
    }

    public void refresh(@NotNull UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            apply(player);
        }
    }

    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void clear(@NotNull Player player) {
        TabManager.getInstance().remove(player);
        NametagManager.getInstance().remove(player);
        BoardManager.getInstance().remove(player);
    }

    @NotNull
    private CoreRank activeRankOrFallback(@NotNull UUID playerUuid) {
        String activeId = grantService.activeRank(playerUuid);
        return rankOrFallback(activeId);
    }

    @NotNull
    private CoreRank rankOrFallback(@NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank != null) {
            return rank;
        }
        rank = rankService.get(FALLBACK_RANK_ID);
        return rank == null ? hardcodedFallback() : rank;
    }

    @NotNull
    private PlaceholderResolver contextFor(@NotNull Player player, @NotNull CoreRank rank) {

        String displayName = DisguiseManager.getInstance().getDisplayName(
                player.getUniqueId(), player.getName());
        return new PlaceholderResolver()
                .set("player", displayName)
                .set("rank", rank.name())
                .set("rank_id", rank.id())
                .set("rank_color", rank.color())
                .set("server", serverId)
                .set("online", Integer.toString(onlineCount()))
                .set("chat_prefix", rank.chatPrefix())
                .set("tab_prefix", rank.tabPrefix())
                .set("nametag_prefix", rank.nametagPrefix())
                .set("prefix", rank.prefix())
                .set("suffix", rank.suffix())

                .set("cosmetic_chat_tag", cosmeticDisplay(player.getUniqueId(), CosmeticType.CHAT_TAG))
                .set("cosmetic_title", cosmeticDisplay(player.getUniqueId(), CosmeticType.TITLE));
    }

    @NotNull
    private static String cosmeticDisplay(@NotNull UUID playerUuid, @NotNull CosmeticType type) {
        String equipped = CosmeticManager.getInstance().getEquipped(playerUuid, type);
        if (equipped == null) {
            return "";
        }
        CosmeticManager.Cosmetic cosmetic = CosmeticManager.getInstance().get(equipped);
        return cosmetic == null ? "" : cosmetic.displayName();
    }

    private void applyTab(@NotNull Player player, @NotNull CoreRank rank, @NotNull PlaceholderResolver ctx) {
        TabManager tab = TabManager.getInstance();

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
                FALLBACK_RANK_ID, "Default", "#D2D8E0",
                org.bukkit.Material.LIGHT_GRAY_DYE,
                "&#D2D8E0", "", 1000, false, false, "",
                List.of(),
                "&#D2D8E0", "%chat_prefix%%player%&7: &#FFFFFF%message%",
                "&#D2D8E0", "%tab_prefix%%player%",
                "&#D2D8E0", "%nametag_prefix%%player%");
    }

    @NotNull
    List<String> tabHeader() { return tabHeader; }

    @NotNull
    List<String> scoreboardLines() { return scoreboardLines; }

    @NotNull
    String scoreboardTitle() { return scoreboardTitle; }

    @NotNull
    public JavaPlugin plugin() { return plugin; }
}

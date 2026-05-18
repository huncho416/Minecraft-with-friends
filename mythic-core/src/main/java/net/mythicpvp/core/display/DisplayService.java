package net.mythicpvp.core.display;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.staffmode.StaffModeService;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
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
import java.util.function.Function;
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
    private StaffModeService staffModeService;
    private Function<UUID, Integer> queuePositionLookup;
    private Function<UUID, net.mythicpvp.core.transfer.TransferQueueService.QueueStatus> queueStatusLookup;

    public void setShardRegistry(@NotNull net.mythicpvp.core.transfer.ShardRegistry registry) {
        // legacy hook; presence counter takes precedence
    }

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

    public void setQueuePositionLookup(@NotNull Function<UUID, Integer> lookup) {
        this.queuePositionLookup = lookup;
    }

    public void setQueueStatusLookup(@NotNull Function<UUID, net.mythicpvp.core.transfer.TransferQueueService.QueueStatus> lookup) {
        this.queueStatusLookup = lookup;
    }

    public void setStaffModeService(@NotNull StaffModeService staffModeService) {
        this.staffModeService = staffModeService;
        TabManager.getInstance().setVisibilityPredicate(staffModeService::canSee);
        NametagManager.getInstance().setVisibilityPredicate(staffModeService::canSee);
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
                .set("cosmetic_title", cosmeticDisplay(player.getUniqueId(), CosmeticType.TITLE))
                .set("queue_position", queuePositionDisplay(player.getUniqueId()))
                .set("queue_section", queueSectionDisplay(player.getUniqueId()))
                .set("ping", pingDisplay(player))
                .set("tps", tpsDisplay());
    }

    @NotNull
    private static String pingDisplay(@NotNull Player player) {
        int ping = player.getPing();
        String color;
        if (ping < 100) color = "&#9CFF9C";
        else if (ping < 200) color = "&#FFEC8A";
        else color = "&#FF8A8A";
        return color + ping + "ms";
    }

    @NotNull
    private String tpsDisplay() {
        double tps = currentTps();
        String color;
        if (tps >= 19.5) color = "&#9CFF9C";
        else if (tps >= 17.0) color = "&#FFEC8A";
        else color = "&#FF8A8A";
        return color + String.format(java.util.Locale.ROOT, "%.2f", Math.min(tps, 20.0));
    }

    private double currentTps() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            return tps.length > 0 ? tps[0] : 20.0;
        } catch (Throwable t) {
            return 20.0;
        }
    }

    @NotNull
    private String queueSectionDisplay(@NotNull UUID playerUuid) {
        if (queueStatusLookup == null) {
            return "";
        }
        net.mythicpvp.core.transfer.TransferQueueService.QueueStatus status = queueStatusLookup.apply(playerUuid);
        if (status == null) {
            return "";
        }
        return "  \n&#F529BEQueue&8: &#FFFFFF#" + status.position()
                + "&7/&#FFFFFF" + status.total()
                + "&8 → &#D2D8E0" + status.shard()
                + "\n   ";
    }

    @NotNull
    private String queuePositionDisplay(@NotNull UUID playerUuid) {
        if (queuePositionLookup == null) {
            return "";
        }
        Integer pos = queuePositionLookup.apply(playerUuid);
        return pos == null ? "" : "#" + pos;
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
        String vanishPrefix = isVanished(player) ? "&7(V) " : "";
        String prefix = sanitizeColors(PapiBridge.apply(player, vanishPrefix + ctx.resolve(rank.tabPrefix())));
        String suffix = sanitizeColors(PapiBridge.apply(player, ctx.resolve(rank.suffix())));
        tab.setLayout(player, header, footer);
        tab.setEntry(player.getUniqueId(),
                prefix,
                suffix,
                rank.weight());
        String visibleName = DisguiseManager.getInstance().getDisplayName(player.getUniqueId(), player.getName());
        player.playerListName(MythicHex.colorize(prefix + visibleName + suffix));
        tab.apply(player);
    }

    private void applyNametag(@NotNull Player player, @NotNull CoreRank rank, @NotNull PlaceholderResolver ctx) {
        NametagManager.getInstance().setNametag(
                player,
                sanitizeColors((isVanished(player) ? "&7(V) " : "") + ctx.resolve(rank.nametagPrefix())),
                sanitizeColors(ctx.resolve(rank.suffix())),
                rank.weight(),
                null);
    }

    @NotNull
    private static String sanitizeColors(@NotNull String input) {
        return input.replaceAll("(?<!&)#([A-Fa-f0-9]{6})", "&#$1");
    }

    private boolean isVanished(@NotNull Player player) {
        StaffModeService staff = staffModeService;
        return staff != null && staff.isVanished(player.getUniqueId());
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
            String papi = PapiBridge.apply(player, line);
            if (papi.contains("\n")) {
                for (String split : papi.split("\n", -1)) {
                    withPapi.add(split);
                }
            } else if (line.contains("%queue_section%") && papi.isEmpty()) {
                // drop empty conditional section
            } else {
                withPapi.add(papi);
            }
        }
        board.setLines(withPapi);
    }

    @NotNull
    IntSupplier onlineCounter() {
        return () -> {
            if (presenceCounter != null) {
                int network = presenceCounter.getAsInt();
                if (network > 0) {
                    return network;
                }
            }
            return Bukkit.getOnlinePlayers().size();
        };
    }

    private IntSupplier presenceCounter;

    public void setPresenceCounter(@NotNull IntSupplier counter) {
        this.presenceCounter = counter;
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
                "&#D2D8E0", "%nametag_prefix%%player%",
                CoreRank.SCOPE_GLOBAL);
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

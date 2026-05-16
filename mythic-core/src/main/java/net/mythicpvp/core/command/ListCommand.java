package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CommandAlias("list|who|playerlist")
public final class ListCommand extends MythicCommand {

    private static final String DEFAULT_RANK_ID = "default";
    private static final String DEFAULT_COLOR = "&#D2D8E0";

    private final RankService ranks;
    private final GrantService grants;

    public ListCommand(@NotNull RankService ranks, @NotNull GrantService grants) {
        this.ranks = ranks;
        this.grants = grants;
    }

    @Default
    public void execute(@NotNull CommandSender sender) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        Map<String, List<Player>> byRank = groupByRank(online);
        sender.sendMessage(MythicHex.colorize(
                "&#F529BE&lOnline players &7(&f" + online.size() + "&7):"));
        if (byRank.isEmpty()) {
            sender.sendMessage(MythicHex.colorize("&7No one online."));
            return;
        }
        for (Map.Entry<String, List<Player>> entry : byRank.entrySet()) {
            CoreRank rank = ranks.get(entry.getKey());
            String rankName = rank != null ? rank.name() : entry.getKey();
            String color = rank != null ? sanitizeColor(rank.color()) : DEFAULT_COLOR;
            List<Player> players = entry.getValue();
            String header = "&8• " + color + rankName + " &7(&f" + players.size() + "&7):";
            StringBuilder line = new StringBuilder("    ");
            for (int i = 0; i < players.size(); i++) {
                if (i > 0) line.append("&7, ");
                line.append(color).append(players.get(i).getName());
            }
            sender.sendMessage(MythicHex.colorize(header));
            sender.sendMessage(MythicHex.colorize(line.toString()));
        }
    }

    @NotNull
    private Map<String, List<Player>> groupByRank(@NotNull Collection<? extends Player> online) {
        Map<String, List<Player>> grouped = new LinkedHashMap<>();
        List<RankBucket> buckets = new ArrayList<>();
        for (Player p : online) {
            String rawRank = grants.activeRank(p.getUniqueId());
            final String rankId = (rawRank == null || rawRank.isEmpty()) ? DEFAULT_RANK_ID : rawRank;
            CoreRank rank = ranks.get(rankId);
            int weight = rank == null ? Integer.MAX_VALUE : rank.weight();
            grouped.computeIfAbsent(rankId, k -> new ArrayList<>()).add(p);
            if (buckets.stream().noneMatch(b -> b.rankId.equals(rankId))) {
                buckets.add(new RankBucket(rankId, weight));
            }
        }
        buckets.sort(Comparator.comparingInt((RankBucket b) -> b.weight).reversed().thenComparing(b -> b.rankId));
        Map<String, List<Player>> sorted = new LinkedHashMap<>();
        for (RankBucket b : buckets) {
            sorted.put(b.rankId, grouped.get(b.rankId));
        }
        return sorted;
    }

    @NotNull
    private static String sanitizeColor(@NotNull String color) {
        if (color.isBlank()) return DEFAULT_COLOR;
        if (color.startsWith("&")) return color;
        if (color.startsWith("#")) return "&" + color;
        return color;
    }

    private record RankBucket(@NotNull String rankId, int weight) {}
}

package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.config.MythicConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RankCosmeticBundles {

    private final Map<String, List<String>> byRankId = new ConcurrentHashMap<>();

    public void load(@NotNull MythicConfig ranksConfig) {
        byRankId.clear();
        ConfigurationSection root = ranksConfig.getConfig().getConfigurationSection("ranks");
        if (root == null) {
            return;
        }
        for (String rankId : root.getKeys(false)) {
            List<String> cosmetics = ranksConfig.getStringList("ranks." + rankId + ".bundled-cosmetics");
            if (!cosmetics.isEmpty()) {
                byRankId.put(rankId.toLowerCase(), List.copyOf(cosmetics));
            }
        }
    }

    @NotNull
    public List<String> bundledFor(@NotNull String rankId) {
        return byRankId.getOrDefault(rankId.toLowerCase(), List.of());
    }

    @NotNull
    public java.util.Set<String> rankIds() {
        return java.util.Set.copyOf(byRankId.keySet());
    }
}

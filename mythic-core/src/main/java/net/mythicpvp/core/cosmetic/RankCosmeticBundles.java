package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.config.MythicConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map of rank id → cosmetic ids automatically granted when the rank is.
 *
 * <p>Loaded from {@code ranks.yml} under each rank's {@code
 * bundled-cosmetics} key:
 *
 * <pre>{@code
 * ranks:
 *   vip:
 *     bundled-cosmetics:
 *       - hat.party_crown
 *       - title.vip
 * }</pre>
 *
 * <p>Kept as a separate registry from {@link
 * net.mythicpvp.core.rank.CoreRank} so the existing record (referenced
 * by tests + STDB DTOs) doesn't grow another field.
 *
 * <p>Lookups are case-insensitive on the rank id.
 */
public final class RankCosmeticBundles {

    private final Map<String, List<String>> byRankId = new ConcurrentHashMap<>();

    /**
     * Reload bundles from the ranks YAML. Idempotent — overwrites all
     * previous entries so a YAML reload picks up adds and deletes.
     */
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

    /** Ids of cosmetics bundled with {@code rankId}. Empty if none. */
    @NotNull
    public List<String> bundledFor(@NotNull String rankId) {
        return byRankId.getOrDefault(rankId.toLowerCase(), List.of());
    }

    /** All rank ids that have at least one bundled cosmetic — for diagnostics. */
    @NotNull
    public java.util.Set<String> rankIds() {
        return java.util.Set.copyOf(byRankId.keySet());
    }
}

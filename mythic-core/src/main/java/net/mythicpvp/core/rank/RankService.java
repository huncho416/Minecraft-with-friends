package net.mythicpvp.core.rank;

import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.permission.PermissionManager;
import net.mythicpvp.suite.permission.Rank;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RankService {

    private final Map<String, CoreRank> ranks = new LinkedHashMap<>();

    public void load(@NotNull MythicConfig config) {
        ranks.clear();
        ConfigurationSection section = config.getConfig().getConfigurationSection("ranks");
        if (section == null) {
            register(fallbackDefault());
            return;
        }
        for (String id : section.getKeys(false)) {
            String path = "ranks." + id + ".";
            List<String> permissions = config.getStringList(path + "permissions");
            CoreRank rank = new CoreRank(
                    normalize(id),
                    config.getString(path + "name", id),
                    config.getString(path + "color", "#808080"),
                    material(config.getString(path + "dye", "LIGHT_GRAY_DYE")),
                    config.getString(path + "prefix", "&7"),
                    config.getString(path + "suffix", ""),
                    config.getInt(path + "weight", 1000),
                    config.getBoolean(path + "staff", false),
                    config.getBoolean(path + "donator", false),
                    normalize(config.getString(path + "parent", "")),
                    List.copyOf(permissions),
                    config.getString(path + "chat-prefix", config.getString(path + "prefix", "&7")),
                    config.getString(path + "chat-format", "%chat_prefix%%player%&7: &f%message%"),
                    config.getString(path + "tab-prefix", config.getString(path + "prefix", "&7")),
                    config.getString(path + "tab-format", "%tab_prefix%%player%"),
                    config.getString(path + "nametag-prefix", config.getString(path + "prefix", "&7")),
                    config.getString(path + "nametag-format", "%nametag_prefix%%player%")
            );
            register(rank);
        }
        if (!ranks.containsKey("default")) {
            register(fallbackDefault());
        }
    }

    public void register(@NotNull CoreRank rank) {
        ranks.put(rank.id(), rank);
        PermissionManager.getInstance().registerRank(new Rank(rank.id(), rank.prefix(), rank.color(), rank.weight(), SetCopy.copy(rank.permissions()), rank.parent().isBlank() ? null : rank.parent()));
    }

    @Nullable
    public CoreRank get(@NotNull String id) {
        return ranks.get(normalize(id));
    }

    @NotNull
    public List<CoreRank> all() {
        return ranks.values().stream()
                .sorted(Comparator.comparingInt(CoreRank::weight))
                .toList();
    }

    @NotNull
    public List<String> ids() {
        return all().stream().map(CoreRank::id).toList();
    }

    @NotNull
    public RankDisplay display(@NotNull String id) {
        CoreRank rank = get(id);
        if (rank == null) {
            rank = get("default");
        }
        if (rank == null) {
            rank = fallbackDefault();
        }
        return new RankDisplay(rank.chatPrefix(), rank.chatFormat(), rank.tabPrefix(), rank.tabFormat(), rank.nametagPrefix(), rank.nametagFormat());
    }

    @NotNull
    private static String normalize(@NotNull String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }

    @NotNull
    private static Material material(@NotNull String name) {
        Material material = Material.matchMaterial(name);
        return material == null ? Material.LIGHT_GRAY_DYE : material;
    }

    @NotNull
    private static CoreRank fallbackDefault() {
        return new CoreRank("default", "Default", "#808080", Material.LIGHT_GRAY_DYE, "&7", "", 1000, false, false, "", List.of("mythic.join"), "&7", "%chat_prefix%%player%&7: &f%message%", "&7", "%tab_prefix%%player%", "&7", "%nametag_prefix%%player%");
    }

    private static final class SetCopy {
        @NotNull
        static java.util.Set<String> copy(@NotNull List<String> values) {
            return new java.util.LinkedHashSet<>(values);
        }
    }
}

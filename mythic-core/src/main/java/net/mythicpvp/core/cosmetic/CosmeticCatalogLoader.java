package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class CosmeticCatalogLoader {

    private final MythicConfig config;
    private final Logger logger;

    public CosmeticCatalogLoader(@NotNull MythicConfig config, @NotNull Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public int load() {
        ConfigurationSection root = config.getConfig().getConfigurationSection("cosmetics");
        if (root == null) return 0;

        CosmeticManager manager = CosmeticManager.getInstance();
        int count = 0;

        for (String typeName : root.getKeys(false)) {
            CosmeticType type;
            try {
                type = CosmeticType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                logger.warning("[cosmetics] Unknown cosmetic type: " + typeName);
                continue;
            }

            ConfigurationSection typeSection = root.getConfigurationSection(typeName);
            if (typeSection == null) continue;

            for (String id : typeSection.getKeys(false)) {
                ConfigurationSection entry = typeSection.getConfigurationSection(id);
                if (entry == null) continue;

                String displayName = entry.getString("display-name", id);
                String description = entry.getString("description", "");
                String modelStr = entry.getString("item-model");
                NamespacedKey itemModel = modelStr != null ? NamespacedKey.fromString(modelStr) : null;
                String rarity = entry.getString("rarity", "COMMON");
                boolean tradable = entry.getBoolean("tradable", true);
                boolean limited = entry.getBoolean("limited", false);
                String format = entry.getString("format");
                boolean animated = entry.getBoolean("animated", false);

                manager.register(new CosmeticManager.Cosmetic(
                        id, displayName, type, description, itemModel, rarity, tradable, limited, format, animated));
                count++;
            }
        }

        logger.info("[cosmetics] Loaded " + count + " cosmetics from catalog");
        return count;
    }
}

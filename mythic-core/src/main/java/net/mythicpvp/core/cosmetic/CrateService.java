package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.api.Currency;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.economy.EconomyManager;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public final class CrateService {

    private final List<CrateDefinition> crates = new CopyOnWriteArrayList<>();
    private final List<CrateRoll> auditLog = new CopyOnWriteArrayList<>();
    private final CosmeticService cosmeticService;
    private final Logger logger;

    public CrateService(@NotNull CosmeticService cosmeticService, @NotNull Logger logger) {
        this.cosmeticService = cosmeticService;
        this.logger = logger;
    }

    public void loadFromConfig(@NotNull MythicConfig config) {
        crates.clear();
        ConfigurationSection root = config.getConfig().getConfigurationSection("crates");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            String displayName = section.getString("display-name", id);
            long cost = section.getLong("cost", 0);
            Currency currency;
            try {
                currency = Currency.valueOf(section.getString("currency", "COINS"));
            } catch (IllegalArgumentException e) {
                logger.warning("[crates] Unknown currency for crate " + id);
                continue;
            }

            List<CrateDefinition.CrateEntry> entries = new ArrayList<>();
            List<?> pool = section.getMapList("pool");
            for (Object obj : pool) {
                if (obj instanceof Map<?, ?> map) {
                    String cosmeticId = String.valueOf(map.get("cosmetic-id"));
                    int weight = map.containsKey("weight") ? ((Number) map.get("weight")).intValue() : 1;
                    entries.add(new CrateDefinition.CrateEntry(cosmeticId, weight));
                }
            }

            long availableFrom = section.getLong("available-from", 0);
            long availableUntil = section.getLong("available-until", 0);

            crates.add(new CrateDefinition(id, displayName, cost, currency, List.copyOf(entries), availableFrom, availableUntil));
        }

        logger.info("[crates] Loaded " + crates.size() + " crate definitions");
    }

    @Nullable
    public CrateDefinition getCrate(@NotNull String id) {
        return crates.stream().filter(c -> c.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    @NotNull
    public List<CrateDefinition> allCrates() {
        return List.copyOf(crates);
    }

    @Nullable
    public CrateRoll roll(@NotNull UUID player, @NotNull CrateDefinition crate) {
        boolean paid = EconomyManager.getInstance().withdraw(player, crate.currency(), crate.cost());
        if (!paid) return null;

        int totalWeight = crate.totalWeight();
        if (totalWeight <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        CrateDefinition.CrateEntry selected = crate.entries().getLast();
        for (CrateDefinition.CrateEntry entry : crate.entries()) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                selected = entry;
                break;
            }
        }

        double rollPercentage = (double) selected.weight() / totalWeight * 100.0;
        CosmeticManager.getInstance().grantCosmetic(player, selected.cosmeticId());
        cosmeticService.persistGrant(player, selected.cosmeticId(), "CRATE", crate.id());

        CrateRoll result = new CrateRoll(player, crate.id(), selected.cosmeticId(), rollPercentage, System.currentTimeMillis());
        auditLog.add(result);
        return result;
    }

    @Nullable
    public CrateRoll rollFree(@NotNull UUID player, @NotNull CrateDefinition crate) {
        int totalWeight = crate.totalWeight();
        if (totalWeight <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        CrateDefinition.CrateEntry selected = crate.entries().getLast();
        for (CrateDefinition.CrateEntry entry : crate.entries()) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                selected = entry;
                break;
            }
        }

        double rollPercentage = (double) selected.weight() / totalWeight * 100.0;
        CosmeticManager.getInstance().grantCosmetic(player, selected.cosmeticId());
        cosmeticService.persistGrant(player, selected.cosmeticId(), "CREDIT_SHOP_CRATE", crate.id());

        CrateRoll result = new CrateRoll(player, crate.id(), selected.cosmeticId(), rollPercentage, System.currentTimeMillis());
        auditLog.add(result);
        return result;
    }

    @NotNull
    public List<CrateRoll> getAuditLog() {
        return List.copyOf(auditLog);
    }
}

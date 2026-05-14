package net.mythicpvp.core.cosmetic;

import net.mythicpvp.core.persistence.PersistenceGateway;
import net.mythicpvp.suite.api.Currency;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.economy.EconomyManager;
import net.mythicpvp.suite.item.MythicItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class CosmeticService {

    private final PersistenceGateway persistence;
    private final NamespacedKey cosmeticKey;

    public CosmeticService(@NotNull PersistenceGateway persistence, @NotNull JavaPlugin plugin) {
        this(persistence, new NamespacedKey(plugin, "cosmetic_id"));
    }

    CosmeticService(@NotNull PersistenceGateway persistence, @NotNull NamespacedKey cosmeticKey) {
        this.persistence = persistence;
        this.cosmeticKey = cosmeticKey;
    }

    public void equip(@NotNull UUID player, @NotNull CosmeticType type, @NotNull String cosmeticId) {
        CosmeticManager.getInstance().equip(player, type, cosmeticId);
        persistence.cosmeticEquip(player, type.name(), cosmeticId);
    }

    public void unequip(@NotNull UUID player, @NotNull CosmeticType type) {
        CosmeticManager.getInstance().unequip(player, type);
    }

    @Nullable
    public ItemStack withdraw(@NotNull UUID player, @NotNull String cosmeticId) {
        CosmeticManager manager = CosmeticManager.getInstance();
        if (!manager.ownsCosmetic(player, cosmeticId)) return null;

        CosmeticManager.Cosmetic cosmetic = manager.get(cosmeticId);
        if (cosmetic == null || !cosmetic.tradable()) return null;

        ItemStack item = MythicItem.create(Material.PAPER)
                .name("&#F529BE" + cosmetic.displayName())
                .lore(List.of(
                        "&7Type: &f" + cosmetic.type().getDisplayName(),
                        "&7Rarity: &f" + cosmetic.rarity(),
                        "&7" + cosmetic.description(),
                        "",
                        "&#D2D8E0Right-click to redeem"))
                .build();

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(cosmeticKey, PersistentDataType.STRING, cosmeticId);
        item.setItemMeta(meta);
        return item;
    }

    public boolean redeem(@NotNull UUID player, @NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String cosmeticId = meta.getPersistentDataContainer().get(cosmeticKey, PersistentDataType.STRING);
        if (cosmeticId == null) return false;

        CosmeticManager manager = CosmeticManager.getInstance();
        if (manager.ownsCosmetic(player, cosmeticId)) return false;

        manager.grantCosmetic(player, cosmeticId);
        persistGrant(player, cosmeticId, "REDEEM", "item");
        return true;
    }

    public boolean purchase(@NotNull UUID player, @NotNull String cosmeticId, @NotNull Currency currency, long price) {
        CosmeticManager manager = CosmeticManager.getInstance();
        if (manager.ownsCosmetic(player, cosmeticId)) return false;

        boolean paid = EconomyManager.getInstance().withdraw(player, currency, price);
        if (!paid) return false;

        manager.grantCosmetic(player, cosmeticId);
        persistGrant(player, cosmeticId, "PURCHASE", currency.name());
        return true;
    }

    public void persistGrant(@NotNull UUID player, @NotNull String cosmeticId, @NotNull String source, @NotNull String reference) {
        CosmeticManager.Cosmetic cosmetic = CosmeticManager.getInstance().get(cosmeticId);
        String type = cosmetic != null ? cosmetic.type().name() : "UNKNOWN";
        persistence.cosmeticGrant(player, cosmeticId, type, source, reference);
    }

    @NotNull
    public NamespacedKey getCosmeticKey() {
        return cosmeticKey;
    }
}

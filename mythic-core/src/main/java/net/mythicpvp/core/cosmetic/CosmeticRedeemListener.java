package net.mythicpvp.core.cosmetic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class CosmeticRedeemListener implements Listener {

    private final CosmeticService cosmeticService;

    public CosmeticRedeemListener(@NotNull CosmeticService cosmeticService) {
        this.cosmeticService = cosmeticService;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(cosmeticService.getCosmeticKey(), PersistentDataType.STRING)) return;

        event.setCancelled(true);

        boolean redeemed = cosmeticService.redeem(player.getUniqueId(), item);
        if (redeemed) {
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("Cosmetic redeemed!");
        } else {
            player.sendMessage("You already own this cosmetic.");
        }
    }
}

package net.mythicpvp.hub.selector;

import net.mythicpvp.suite.item.MythicItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class HubItemListener implements Listener {

    private static final int SELECTOR_SLOT = 4;

    private final ServerSelectorMenu selectorMenu;
    private final NamespacedKey selectorKey;

    public HubItemListener(@NotNull JavaPlugin plugin, @NotNull ServerSelectorMenu selectorMenu) {
        this.selectorMenu = selectorMenu;
        this.selectorKey = new NamespacedKey(plugin, "server_selector");
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        giveSelector(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        giveSelector(event.getPlayer());
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isSelector(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        selectorMenu.openGroupMenu(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(@NotNull PlayerDropItemEvent event) {
        if (isSelector(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isSelector(current) || isSelector(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (isSelector(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(@NotNull PlayerSwapHandItemsEvent event) {
        if (isSelector(event.getMainHandItem()) || isSelector(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    private void giveSelector(@NotNull Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (i == SELECTOR_SLOT) {
                continue;
            }
            if (isSelector(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
        player.getInventory().setItem(SELECTOR_SLOT, selectorItem());
    }

    private boolean isSelector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(selectorKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @NotNull
    private ItemStack selectorItem() {
        ItemStack item = MythicItem.create(Material.COMPASS)
                .name("&#F529BEServer Selector")
                .lore(List.of("&#D2D8E0Right click to browse servers"))
                .build();
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(selectorKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}

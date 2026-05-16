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

    private static final int SELECTOR_SLOT = 0;
    private static final int HUB_SLOT = 8;

    private final ServerSelectorMenu selectorMenu;
    private final HubListMenuService hubListMenu;
    private final NamespacedKey selectorKey;
    private final NamespacedKey hubKey;

    public HubItemListener(@NotNull JavaPlugin plugin,
                           @NotNull ServerSelectorMenu selectorMenu,
                           @NotNull HubListMenuService hubListMenu) {
        this.selectorMenu = selectorMenu;
        this.hubListMenu = hubListMenu;
        this.selectorKey = new NamespacedKey(plugin, "server_selector");
        this.hubKey = new NamespacedKey(plugin, "hub_selector");
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        giveItems(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        giveItems(event.getPlayer());
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (isSelector(item)) {
            event.setCancelled(true);
            selectorMenu.openGroupMenu(event.getPlayer());
            return;
        }
        if (isHubItem(item)) {
            event.setCancelled(true);
            hubListMenu.open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(@NotNull PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (isSelector(stack) || isHubItem(stack)) {
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
        if (isSelector(current) || isSelector(cursor) || isHubItem(current) || isHubItem(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        ItemStack cursor = event.getOldCursor();
        if (isSelector(cursor) || isHubItem(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(@NotNull PlayerSwapHandItemsEvent event) {
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        if (isSelector(main) || isSelector(off) || isHubItem(main) || isHubItem(off)) {
            event.setCancelled(true);
        }
    }

    private void giveItems(@NotNull Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (i == SELECTOR_SLOT || i == HUB_SLOT) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (isSelector(stack) || isHubItem(stack)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.getInventory().setItem(SELECTOR_SLOT, selectorItem());
        player.getInventory().setItem(HUB_SLOT, hubItem());
    }

    private boolean isSelector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(selectorKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private boolean isHubItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(hubKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @NotNull
    private ItemStack selectorItem() {
        ItemStack item = MythicItem.create(Material.COMPASS)
                .name("&#F529BEServer Selector")
                .lore(List.of("&#D2D8E0Right click to browse gamemodes"))
                .build();
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(selectorKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    private ItemStack hubItem() {
        ItemStack item = MythicItem.create(Material.BEACON)
                .name("&#F529BEHub Selector")
                .lore(List.of("&#D2D8E0Right click to browse hubs"))
                .build();
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(hubKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}

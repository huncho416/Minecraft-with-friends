package net.mythicpvp.hub.selector;

import net.mythicpvp.suite.item.MythicItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class HubItemListener implements Listener {

    private final ServerSelectorMenu selectorMenu;
    private final NamespacedKey selectorKey;

    public HubItemListener(@NotNull JavaPlugin plugin, @NotNull ServerSelectorMenu selectorMenu) {
        this.selectorMenu = selectorMenu;
        this.selectorKey = new NamespacedKey(plugin, "server_selector");
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setItem(0, selectorItem());
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) {
            return;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(selectorKey, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) {
            return;
        }
        event.setCancelled(true);
        selectorMenu.openGroupMenu(event.getPlayer());
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

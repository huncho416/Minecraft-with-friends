package net.mythicpvp.suite.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class MenuListener implements Listener {

    private static final Map<InventoryClickEvent, Boolean> HANDLED =
            Collections.synchronizedMap(new WeakHashMap<>());

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (HANDLED.put(event, Boolean.TRUE) != null) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MythicMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof PaginatedMenu paginated) {
            paginated.handleClick(event);
        }
    }
}

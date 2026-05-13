package net.mythicpvp.suite.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MythicMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof PaginatedMenu paginated) {
            paginated.handleClick(event);
        }
    }
}

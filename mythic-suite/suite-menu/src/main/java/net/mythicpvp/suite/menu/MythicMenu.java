package net.mythicpvp.suite.menu;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class MythicMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private Consumer<InventoryClickEvent> globalClickHandler;
    private boolean cancelClicks = true;
    private long clickCooldownMillis;

    private MythicMenu(int rows, @NotNull String title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, MythicHex.colorize(title));
    }

    @NotNull
    public static MythicMenu create(int rows, @NotNull String title) {
        return new MythicMenu(rows, title);
    }

    @NotNull
    public MythicMenu slot(int slot, @NotNull ItemStack item) {
        inventory.setItem(slot, item);
        return this;
    }

    @NotNull
    public MythicMenu slot(int slot, @NotNull ItemStack item, @NotNull Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        clickHandlers.put(slot, handler);
        return this;
    }

    @NotNull
    public MythicMenu fill(@NotNull ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
        return this;
    }

    @NotNull
    public MythicMenu border(@NotNull ItemStack item) {
        int size = inventory.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, item);
            inventory.setItem(size - 9 + i, item);
        }
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, item);
            inventory.setItem(i * 9 + 8, item);
        }
        return this;
    }

    @NotNull
    public MythicMenu onClick(@NotNull Consumer<InventoryClickEvent> handler) {
        this.globalClickHandler = handler;
        return this;
    }

    @NotNull
    public MythicMenu cancelClicks(boolean cancel) {
        this.cancelClicks = cancel;
        return this;
    }

    @NotNull
    public MythicMenu clickCooldown(long millis) {
        this.clickCooldownMillis = Math.max(0, millis);
        return this;
    }

    public void handleClick(@NotNull InventoryClickEvent event) {
        if (cancelClicks) event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player && clickCooldownMillis > 0) {
            long now = System.currentTimeMillis();
            long nextClick = clickCooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (now < nextClick) {
                return;
            }
            clickCooldowns.put(player.getUniqueId(), now + clickCooldownMillis);
        }

        Consumer<InventoryClickEvent> slotHandler = clickHandlers.get(event.getRawSlot());
        if (slotHandler != null) {
            slotHandler.accept(event);
        } else if (globalClickHandler != null) {
            globalClickHandler.accept(event);
        }
    }

    public void open(@NotNull Player player) {
        try {
            player.openInventory(inventory);
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[mythic-menu] openInventory failed for "
                    + player.getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isCancelClicks() {
        return cancelClicks;
    }
}

package net.mythicpvp.suite.menu;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MythicMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    private Consumer<InventoryClickEvent> globalClickHandler;
    private boolean cancelClicks = true;

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

    public void handleClick(@NotNull InventoryClickEvent event) {
        if (cancelClicks) event.setCancelled(true);

        Consumer<InventoryClickEvent> slotHandler = clickHandlers.get(event.getRawSlot());
        if (slotHandler != null) {
            slotHandler.accept(event);
        } else if (globalClickHandler != null) {
            globalClickHandler.accept(event);
        }
    }

    public void open(@NotNull Player player) {
        player.openInventory(inventory);
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

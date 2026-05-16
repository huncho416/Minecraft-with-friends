package net.mythicpvp.suite.menu;

import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PaginatedMenu implements InventoryHolder {

    private final String title;
    private final int rows;
    private final List<ItemStack> items = new ArrayList<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> staticSlots = new HashMap<>();
    private final Map<Integer, ItemStack> staticItems = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> itemClickHandlers = new HashMap<>();
    private int currentPage = 0;
    private Inventory inventory;

    private final int[] contentSlots;

    private PaginatedMenu(int rows, @NotNull String title, int[] contentSlots) {
        this.rows = rows;
        this.title = title;
        this.contentSlots = contentSlots;
    }

    @NotNull
    public static PaginatedMenu create(int rows, @NotNull String title) {
        int size = rows * 9;
        List<Integer> slots = new ArrayList<>();
        for (int i = 9; i < size - 9; i++) {
            int col = i % 9;
            if (col != 0 && col != 8) {
                slots.add(i);
            }
        }
        return new PaginatedMenu(rows, title, slots.stream().mapToInt(Integer::intValue).toArray());
    }

    @NotNull
    public PaginatedMenu addItem(@NotNull ItemStack item) {
        items.add(item);
        return this;
    }

    @NotNull
    public PaginatedMenu addItem(@NotNull ItemStack item, @NotNull Consumer<InventoryClickEvent> handler) {
        int index = items.size();
        items.add(item);
        itemClickHandlers.put(index, handler);
        return this;
    }

    @NotNull
    public PaginatedMenu addItems(@NotNull List<ItemStack> items) {
        this.items.addAll(items);
        return this;
    }

    @NotNull
    public PaginatedMenu staticSlot(int slot, @NotNull ItemStack item, @NotNull Consumer<InventoryClickEvent> handler) {
        staticSlots.put(slot, handler);
        staticItems.put(slot, item);
        return this;
    }

    public int getMaxPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / contentSlots.length));
    }

    public void open(@NotNull Player player) {
        open(player, 0);
    }

    public void open(@NotNull Player player, int page) {
        int maxPages = Math.max(1, getMaxPages());
        this.currentPage = Math.max(0, Math.min(page, maxPages - 1));
        String pageTitle = title + " &#808080(" + (currentPage + 1) + "/" + maxPages + ")";
        this.inventory = Bukkit.createInventory(this, rows * 9, MythicHex.colorize(pageTitle));
        render();
        try {
            player.openInventory(inventory);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[mythic-menu] paginated openInventory failed for "
                    + player.getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void render() {
        inventory.clear();

        int startIndex = currentPage * contentSlots.length;
        for (int i = 0; i < contentSlots.length; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < items.size()) {
                inventory.setItem(contentSlots[i], items.get(itemIndex));
            }
        }

        if (currentPage > 0) {
            inventory.setItem(rows * 9 - 9, MythicItem.create(Material.ARROW).name("&#FF00F8<- Previous Page").build());
        }
        if (currentPage < getMaxPages() - 1) {
            inventory.setItem(rows * 9 - 1, MythicItem.create(Material.ARROW).name("&#FF00F8Next Page ->").build());
        }

        staticItems.forEach((slot, item) -> inventory.setItem(slot, item));
    }

    public void handleClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        if (slot == rows * 9 - 9 && currentPage > 0) {
            open(player, currentPage - 1);
            return;
        }
        if (slot == rows * 9 - 1 && currentPage < getMaxPages() - 1) {
            open(player, currentPage + 1);
            return;
        }

        Consumer<InventoryClickEvent> staticHandler = staticSlots.get(slot);
        if (staticHandler != null) {
            staticHandler.accept(event);
            return;
        }

        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                int itemIndex = currentPage * contentSlots.length + i;
                Consumer<InventoryClickEvent> handler = itemClickHandlers.get(itemIndex);
                if (handler != null) {
                    handler.accept(event);
                }
                return;
            }
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}

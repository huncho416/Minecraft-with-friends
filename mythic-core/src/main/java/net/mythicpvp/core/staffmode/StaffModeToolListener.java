package net.mythicpvp.core.staffmode;

import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffModeToolListener implements Listener {

    public static final String INSPECT_VIEW_PERMISSION = "mythic.core.staffmode.inspect.view";
    public static final String INSPECT_EDIT_PERMISSION = "mythic.core.staffmode.inspect.edit";

    private final StaffModeService staff;
    private final CoreMessages messages;
    private final Map<UUID, UUID> inspectingViewers = new ConcurrentHashMap<>();

    public StaffModeToolListener(
            @NotNull StaffModeService staff,
            @NotNull CoreMessages messages,
            @SuppressWarnings("unused") @NotNull net.mythicpvp.core.rank.GrantService grants,
            @SuppressWarnings("unused") @NotNull net.mythicpvp.core.rank.RankService ranks) {
        this.staff = staff;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!staff.inStaffMode(player.getUniqueId())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        StaffModeTool tool = matchTool(item);
        if (tool == null) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        event.setCancelled(true);
        switch (tool.type()) {
            case "INSPECT" -> handleInspect(player, target);
            case "FREEZE" -> handleFreeze(player, target);
            default -> {  }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!staff.inStaffMode(player.getUniqueId())) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        StaffModeTool tool = matchTool(item);
        if (tool == null) {
            return;
        }

        switch (tool.type()) {
            case "RANDOM_TELEPORT" -> {
                event.setCancelled(true);
                handleRandomTeleport(player);
            }
            case "DISABLE" -> {
                event.setCancelled(true);
                staff.disable(player);
                player.sendMessage(messages.component(
                        "messages.staff-mode.disabled",
                        "&#9CFF9CStaff mode disabled."));
            }
            default -> {  }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!staff.isFrozen(event.getPlayer().getUniqueId())) {
            return;
        }

        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        staff.refreshVisibility();
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {

        staff.onQuit(event.getPlayer());
        staff.refreshVisibility();
        inspectingViewers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInspectClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID targetUuid = inspectingViewers.get(viewer.getUniqueId());
        if (targetUuid == null) return;
        if (!sameInventory(event.getView().getTopInventory(), targetUuid)) {
            return;
        }
        if (!viewer.hasPermission(INSPECT_EDIT_PERMISSION)) {
            event.setCancelled(true);
            return;
        }
        InventoryAction action = event.getAction();
        if (action == InventoryAction.NOTHING || action == InventoryAction.UNKNOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInspectDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID targetUuid = inspectingViewers.get(viewer.getUniqueId());
        if (targetUuid == null) return;
        if (!sameInventory(event.getView().getTopInventory(), targetUuid)) {
            return;
        }
        if (!viewer.hasPermission(INSPECT_EDIT_PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInspectClose(@NotNull org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        inspectingViewers.remove(viewer.getUniqueId());
    }

    private boolean sameInventory(@NotNull Inventory top, @NotNull UUID expectedTargetUuid) {
        if (!(top.getHolder() instanceof Player holder)) return false;
        return holder.getUniqueId().equals(expectedTargetUuid);
    }

    private void handleInspect(@NotNull Player staffPlayer, @NotNull Player target) {
        if (!staffPlayer.hasPermission(INSPECT_VIEW_PERMISSION)) {
            staffPlayer.sendMessage(messages.component(
                    "messages.staff-mode.inspect-no-permission",
                    "&#FF8A8AYou don't have permission to inspect inventories."));
            return;
        }
        inspectingViewers.put(staffPlayer.getUniqueId(), target.getUniqueId());
        staffPlayer.openInventory(target.getInventory());
    }

    private void handleFreeze(@NotNull Player staffPlayer, @NotNull Player target) {
        boolean nowFrozen = staff.toggleFreeze(target.getUniqueId());
        staffPlayer.sendMessage(messages.component(
                nowFrozen ? "messages.staff-mode.frozen" : "messages.staff-mode.unfrozen",
                nowFrozen
                        ? "&#9CFF9CFroze &#FFFFFF%target%&#9CFF9C."
                        : "&#9CFF9CUnfroze &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", target.getName())));
    }

    private void handleRandomTeleport(@NotNull Player staffPlayer) {
        Set<UUID> exclude = new HashSet<>();
        exclude.add(staffPlayer.getUniqueId());
        var candidates = staffPlayer.getServer().getOnlinePlayers().stream()
                .filter(p -> !exclude.contains(p.getUniqueId()))
                .filter(p -> staff.canSee(staffPlayer, p))
                .toList();
        if (candidates.isEmpty()) {
            staffPlayer.sendMessage(messages.component(
                    "messages.staff-mode.no-targets",
                    "&#FFEC8ANo other players are online to teleport to."));
            return;
        }
        Player chosen = candidates.get((int) (Math.random() * candidates.size()));
        staffPlayer.teleportAsync(chosen.getLocation());
        staffPlayer.sendMessage(messages.component(
                "messages.staff-mode.random-teleport",
                "&#9CFF9CTeleported to &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", chosen.getName())));
    }

    @Nullable
    private StaffModeTool matchTool(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        NamespacedKey key = staff.toolKey();
        if (key == null) {
            return null;
        }
        String type = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (type == null) {
            return null;
        }
        return staff.toolByType(type);
    }
}

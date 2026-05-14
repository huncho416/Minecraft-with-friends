package net.mythicpvp.core.staffmode;

import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffModeService {

    private final Map<UUID, StaffModeSnapshot> snapshots = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> frozen = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean vanish = true;
    private volatile boolean fly = true;
    private volatile List<StaffModeTool> tools = List.of();

    public void load(@NotNull MythicConfig config) {
        this.vanish = config.getBoolean("staff-mode.vanish", true);
        this.fly = config.getBoolean("staff-mode.fly", true);
        this.tools = parseTools(config);
    }

    public boolean inStaffMode(@NotNull UUID player) {
        return snapshots.containsKey(player);
    }

    public boolean toggle(@NotNull Player player) {
        if (snapshots.containsKey(player.getUniqueId())) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    public void enable(@NotNull Player player) {
        if (snapshots.containsKey(player.getUniqueId())) {
            return;
        }
        snapshots.put(player.getUniqueId(), capture(player));
        applyStaffState(player);
    }

    public void disable(@NotNull Player player) {
        StaffModeSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        restore(player, snapshot);
    }

    public void onQuit(@NotNull Player player) {
        StaffModeSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot != null) {
            restore(player, snapshot);
        }
        frozen.remove(player.getUniqueId());
    }

    public boolean toggleFreeze(@NotNull UUID target) {
        if (frozen.contains(target)) {
            frozen.remove(target);
            return false;
        }
        frozen.add(target);
        return true;
    }

    public boolean isFrozen(@NotNull UUID target) {
        return frozen.contains(target);
    }

    @NotNull
    public List<StaffModeTool> tools() {
        return tools;
    }

    public boolean vanishEnabled() {
        return vanish;
    }

    public boolean flyEnabled() {
        return fly;
    }

    @NotNull
    private StaffModeSnapshot capture(@NotNull Player player) {
        PlayerInventory inv = player.getInventory();
        return new StaffModeSnapshot(
                inv.getContents().clone(),
                inv.getArmorContents().clone(),
                inv.getItemInOffHand().clone(),
                player.getGameMode(),
                player.getAllowFlight(),
                player.isFlying());
    }

    private void applyStaffState(@NotNull Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        for (StaffModeTool tool : tools) {
            if (tool.slot() < 0 || tool.slot() > 8) {
                continue;
            }
            ItemStack stack = new ItemStack(tool.material());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(MythicHex.colorize(tool.name()));
                stack.setItemMeta(meta);
            }
            inv.setItem(tool.slot(), stack);
        }
        if (fly) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        if (vanish) {
            applyVanish(player);
        }
        player.setGameMode(GameMode.CREATIVE);
    }

    private void restore(@NotNull Player player, @NotNull StaffModeSnapshot snapshot) {
        PlayerInventory inv = player.getInventory();
        inv.setContents(snapshot.contents());
        inv.setArmorContents(snapshot.armor());
        if (snapshot.offhand() != null) {
            inv.setItemInOffHand(snapshot.offhand());
        }
        player.setGameMode(snapshot.gameMode());
        player.setAllowFlight(snapshot.allowFlight());
        player.setFlying(snapshot.flying());
        if (vanish) {
            unapplyVanish(player);
        }
    }

    private void applyVanish(@NotNull Player staff) {

        for (Player viewer : staff.getServer().getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(staff.getUniqueId())) {
                continue;
            }
            if (!viewer.hasPermission(net.mythicpvp.core.staff.StaffPresenceListener.STAFF_PERMISSION)) {

                boolean hidden = hidePlayerLegacy(viewer, staff);

                if (!hidden) {  }
            }
        }
    }

    private void unapplyVanish(@NotNull Player staff) {
        for (Player viewer : staff.getServer().getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(staff.getUniqueId())) {
                continue;
            }
            boolean shown = showPlayerLegacy(viewer, staff);
            if (!shown) {  }
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean hidePlayerLegacy(@NotNull Player viewer, @NotNull Player target) {
        viewer.hidePlayer(target);
        return true;
    }

    @SuppressWarnings("deprecation")
    private static boolean showPlayerLegacy(@NotNull Player viewer, @NotNull Player target) {
        viewer.showPlayer(target);
        return true;
    }

    @NotNull
    private static List<StaffModeTool> parseTools(@NotNull MythicConfig config) {
        List<StaffModeTool> list = new ArrayList<>();
        ConfigurationSection root = config.getConfig().getConfigurationSection("staff-mode");
        if (root == null) {
            return List.of();
        }
        List<Map<?, ?>> raw = root.getMapList("tools");
        for (Map<?, ?> entryWildcard : raw) {

            @SuppressWarnings("unchecked")
            Map<Object, Object> entry = (Map<Object, Object>) entryWildcard;
            Object slotObj = entry.get("slot");
            int slot = (slotObj instanceof Number n) ? n.intValue() : -1;
            String materialName = entry.containsKey("material")
                    ? String.valueOf(entry.get("material")) : "BARRIER";
            String name = entry.containsKey("name")
                    ? String.valueOf(entry.get("name")) : "&7Tool";
            String type = entry.containsKey("type")
                    ? String.valueOf(entry.get("type")) : "DISABLE";
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                continue;
            }
            list.add(new StaffModeTool(slot, material, name, type));
        }
        return List.copyOf(list);
    }
}

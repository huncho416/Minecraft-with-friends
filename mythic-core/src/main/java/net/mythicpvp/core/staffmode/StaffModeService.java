package net.mythicpvp.core.staffmode;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffModeService {

    private final Map<UUID, StaffModeSnapshot> snapshots = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> vanished = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<UUID> frozen = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile @Nullable JavaPlugin plugin;
    private volatile @Nullable RankService ranks;
    private volatile @Nullable GrantService grants;
    private volatile Runnable displayRefresher = () -> {};
    private volatile boolean vanish = true;
    private volatile boolean fly = true;
    private volatile List<StaffModeTool> tools = List.of();

    public void load(@NotNull MythicConfig config) {
        this.vanish = config.getBoolean("staff-mode.vanish", true);
        this.fly = config.getBoolean("staff-mode.fly", true);
        this.tools = parseTools(config);
    }

    public void configureVisibility(
            @NotNull JavaPlugin plugin,
            @NotNull RankService ranks,
            @NotNull GrantService grants,
            @NotNull Runnable displayRefresher) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.grants = grants;
        this.displayRefresher = displayRefresher;
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

    @Nullable
    public NamespacedKey toolKey() {
        JavaPlugin p = plugin;
        return p == null ? null : new NamespacedKey(p, "staff_mode_tool");
    }

    @Nullable
    public StaffModeTool toolByType(@NotNull String type) {
        for (StaffModeTool tool : tools) {
            if (tool.type().equalsIgnoreCase(type)) {
                return tool;
            }
        }
        return null;
    }

    public boolean vanishEnabled() {
        return vanish;
    }

    public boolean flyEnabled() {
        return fly;
    }

    public boolean isVanished(@NotNull UUID player) {
        return vanished.contains(player);
    }

    public boolean toggleVanish(@NotNull Player player) {
        if (vanished.contains(player.getUniqueId())) {
            unapplyVanish(player);
            return false;
        }
        applyVanish(player);
        return true;
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
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        NamespacedKey key = toolKey();
        for (StaffModeTool tool : tools) {
            if (tool.slot() < 0 || tool.slot() > 8) {
                continue;
            }
            ItemStack stack = new ItemStack(tool.material());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(MythicHex.colorize(tool.name()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                if (key != null) {
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, tool.type());
                }
                stack.setItemMeta(meta);
            }
            inv.setItem(tool.slot(), stack);
        }
        if (fly) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        applyVanish(player);
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
        if (vanished.contains(player.getUniqueId())) {
            unapplyVanish(player);
        }
    }

    private void applyVanish(@NotNull Player staff) {
        vanished.add(staff.getUniqueId());
        refreshVisibility();
    }

    private void unapplyVanish(@NotNull Player staff) {
        vanished.remove(staff.getUniqueId());
        refreshVisibility();
    }

    public void refreshVisibility() {
        JavaPlugin currentPlugin = plugin;
        for (Player viewer : org.bukkit.Bukkit.getOnlinePlayers()) {
            for (Player target : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (viewer.getUniqueId().equals(target.getUniqueId())) {
                    continue;
                }
                if (canSee(viewer, target)) {
                    showPlayer(viewer, target, currentPlugin);
                } else {
                    hidePlayer(viewer, target, currentPlugin);
                }
            }
        }
        displayRefresher.run();
    }

    public boolean canSee(@NotNull Player viewer, @NotNull Player target) {
        if (!vanished.contains(target.getUniqueId())) {
            return true;
        }
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return true;
        }
        return rankWeight(viewer.getUniqueId()) > rankWeight(target.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    private static void hidePlayer(@NotNull Player viewer, @NotNull Player target, @Nullable JavaPlugin plugin) {
        if (plugin != null) {
            viewer.hidePlayer(plugin, target);
        } else {
            viewer.hidePlayer(target);
        }
    }

    @SuppressWarnings("deprecation")
    private static void showPlayer(@NotNull Player viewer, @NotNull Player target, @Nullable JavaPlugin plugin) {
        if (plugin != null) {
            viewer.showPlayer(plugin, target);
        } else {
            viewer.showPlayer(target);
        }
    }

    private int rankWeight(@NotNull UUID player) {
        RankService rankService = ranks;
        GrantService grantService = grants;
        if (rankService == null || grantService == null) {
            return 0;
        }
        CoreRank rank = rankService.get(grantService.activeRank(player));
        if (rank == null || !rank.staff()) {
            return 0;
        }
        return rank.weight();
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

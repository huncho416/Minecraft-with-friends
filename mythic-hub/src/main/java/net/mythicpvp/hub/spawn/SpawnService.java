package net.mythicpvp.hub.spawn;

import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpawnService {

    private final JavaPlugin plugin;
    private Location spawnLocation;
    private double voidTeleportY;

    public SpawnService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(@NotNull MythicConfig config) {
        String worldName = config.getString("spawn.world", "world");
        double x = config.getDouble("spawn.x", 0.5);
        double y = config.getDouble("spawn.y", 100.0);
        double z = config.getDouble("spawn.z", 0.5);
        float yaw = (float) config.getDouble("spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("spawn.pitch", 0.0);
        voidTeleportY = config.getDouble("spawn.void-teleport-y", -10.0);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().getFirst();
        }
        spawnLocation = new Location(world, x, y, z, yaw, pitch);
    }

    public void teleportToSpawn(@NotNull Player player) {
        if (spawnLocation == null) return;
        MythicScheduler.runOnEntity(plugin, player, () -> player.teleportAsync(spawnLocation));
    }

    public boolean isBelowVoid(@NotNull Player player) {
        return player.getLocation().getY() < voidTeleportY;
    }

    @Nullable
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public double getVoidTeleportY() {
        return voidTeleportY;
    }
}

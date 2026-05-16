package net.mythicpvp.core.command;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

@CommandAlias("setspawn")
@CommandPermission("mythic.core.spawn.set")
public final class SetSpawnCommand extends MythicCommand {

    private final MythicConfig spawnConfig;

    public SetSpawnCommand(@NotNull MythicConfig spawnConfig) {
        this.spawnConfig = spawnConfig;
    }

    @Default
    public void execute(@NotNull Player player) {
        Location loc = player.getLocation();
        spawnConfig.set("spawn.world", loc.getWorld().getName());
        spawnConfig.set("spawn.x", loc.getX());
        spawnConfig.set("spawn.y", loc.getY());
        spawnConfig.set("spawn.z", loc.getZ());
        spawnConfig.set("spawn.yaw", loc.getYaw());
        spawnConfig.set("spawn.pitch", loc.getPitch());
        spawnConfig.save();
        loc.getWorld().setSpawnLocation(loc);
        boolean hubUpdated = tryUpdateHubSpawn(loc);
        player.sendMessage(MythicHex.colorize(
                String.format("&#9CFF9CSpawn set to &f%s %.2f, %.2f, %.2f&#9CFF9C.",
                        loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ())));
        player.sendMessage(MythicHex.colorize(hubUpdated
                ? "&#9CFF9CHub on-join teleport now uses this spawn (in-memory; edit hub.yml to persist across restarts)."
                : "&#FFEC8AHub plugin not detected on this shard; only world spawn was updated."));
    }

    private static boolean tryUpdateHubSpawn(@NotNull Location loc) {
        RegisteredServiceProvider<?> rsp = lookupHubSpawnService();
        if (rsp == null) return false;
        Object service = rsp.getProvider();
        try {
            Method setter = service.getClass().getMethod("setSpawnLocation", Location.class);
            setter.invoke(service, loc);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static RegisteredServiceProvider<?> lookupHubSpawnService() {
        try {
            Class<?> serviceClass = Class.forName("net.mythicpvp.hub.spawn.SpawnService");
            return Bukkit.getServicesManager().getRegistration(serviceClass);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}

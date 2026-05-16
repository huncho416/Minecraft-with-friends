package net.mythicpvp.core.command;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CSpawn set to &f" + loc.getWorld().getName()
                        + String.format(" %.2f, %.2f, %.2f&#9CFF9C.", loc.getX(), loc.getY(), loc.getZ())));
        player.sendMessage(MythicHex.colorize(
                "&7Hub spawn-on-join uses &fhub.yml/spawn&7; this command also writes &fspawn.yml &7for reference."));
    }
}

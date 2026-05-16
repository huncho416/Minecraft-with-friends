package net.mythicpvp.hub.command;

import net.mythicpvp.hub.safety.BuildModeService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("buildmode")
@CommandPermission("mythic.hub.buildmode")
public final class BuildModeCommand extends MythicCommand {

    private final BuildModeService buildMode;

    public BuildModeCommand(@NotNull BuildModeService buildMode) {
        this.buildMode = buildMode;
    }

    @Default
    public void execute(@NotNull Player player) {
        boolean now = buildMode.toggle(player.getUniqueId());
        player.sendMessage(MythicHex.colorize(
                now
                        ? "&#9CFF9CBuild mode enabled — break/place/containers unlocked."
                        : "&#FF8A8ABuild mode disabled — back to safe-hub rules."));
    }
}

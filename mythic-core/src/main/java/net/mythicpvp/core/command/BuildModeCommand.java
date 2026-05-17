package net.mythicpvp.core.command;

import net.mythicpvp.core.mode.BuildPvpListener;
import net.mythicpvp.core.mode.PlayerModeService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("buildmode|build")
@CommandPermission(BuildPvpListener.BUILD_BYPASS)
public final class BuildModeCommand extends MythicCommand {

    private final PlayerModeService modes;

    public BuildModeCommand(@NotNull PlayerModeService modes) {
        this.modes = modes;
    }

    @Default
    public void execute(@NotNull Player player) {
        boolean enabled = modes.toggleBuild(player);
        player.sendMessage(MythicHex.colorize(enabled
                ? "&#9CFF9CBuild mode &#FFFFFFenabled&#9CFF9C. You can break and place blocks."
                : "&#FF8A8ABuild mode &#FFFFFFdisabled&#FF8A8A."));
    }
}

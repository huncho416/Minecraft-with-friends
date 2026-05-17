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

@CommandAlias("pvpmode|pvp")
@CommandPermission(BuildPvpListener.PVP_BYPASS)
public final class PvpModeCommand extends MythicCommand {

    private final PlayerModeService modes;

    public PvpModeCommand(@NotNull PlayerModeService modes) {
        this.modes = modes;
    }

    @Default
    public void execute(@NotNull Player player) {
        boolean enabled = modes.togglePvp(player);
        player.sendMessage(MythicHex.colorize(enabled
                ? "&#9CFF9CPvP mode &#FFFFFFenabled&#9CFF9C. You can fight other PvP-mode players."
                : "&#FF8A8APvP mode &#FFFFFFdisabled&#FF8A8A."));
    }
}

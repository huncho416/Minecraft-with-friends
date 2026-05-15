package net.mythicpvp.core.command;

import net.mythicpvp.core.staffmode.StaffModeService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("vanish|v")
@CommandPermission("mythic.core.vanish")
public final class VanishCommand extends MythicCommand {

    private final StaffModeService staffMode;

    public VanishCommand(@NotNull StaffModeService staffMode) {
        this.staffMode = staffMode;
    }

    @Default
    public void execute(@NotNull Player player) {
        if (staffMode.inStaffMode(player.getUniqueId()) && staffMode.isVanished(player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF9C9CYou cannot unvanish while staff mode is enabled."));
            return;
        }
        boolean vanished = staffMode.toggleVanish(player);
        player.sendMessage(MythicHex.colorize(vanished
                ? "&#9CFF9CYou are now vanished."
                : "&#9CFF9CYou are no longer vanished."));
    }
}

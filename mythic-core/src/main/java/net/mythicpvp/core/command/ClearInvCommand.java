package net.mythicpvp.core.command;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("clearinv")
@CommandPermission("mythic.core.clearinv")
public final class ClearInvCommand extends MythicCommand {

    @Default
    public void execute(@NotNull Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.sendMessage(MythicHex.colorize("&#9CFF9CInventory cleared."));
    }
}

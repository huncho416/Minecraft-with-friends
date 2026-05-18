package net.mythicpvp.core.command;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("disguiseviewself|dview|disguisesee")
@CommandPermission("mythic.core.disguise.viewself")
public final class DisguiseViewSelfCommand extends MythicCommand {

    @Default
    public void execute(@NotNull Player sender) {
        boolean current = DisguiseManager.getInstance().canSeeThrough(sender.getUniqueId());
        DisguiseManager.getInstance().setStaffView(sender.getUniqueId(), !current);
        if (!current) {
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CSee-through disguises &#9CFF9C&lON&#9CFF9C. You will now see real names beside disguises."));
        } else {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8ASee-through disguises &#FF8A8A&lOFF&#FF8A8A. Disguises will hide real names."));
        }
    }
}

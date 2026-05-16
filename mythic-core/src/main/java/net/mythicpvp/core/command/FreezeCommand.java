package net.mythicpvp.core.command;

import net.mythicpvp.core.staffmode.StaffModeService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("freeze")
@CommandPermission("mythic.core.staffmode.freeze")
public final class FreezeCommand extends MythicCommand {

    private final StaffModeService staffMode;

    public FreezeCommand(@NotNull StaffModeService staffMode) {
        this.staffMode = staffMode;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull CommandSender sender, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/freeze <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AThat player is not online."));
            return;
        }
        boolean nowFrozen = staffMode.toggleFreeze(target.getUniqueId());
        sender.sendMessage(MythicHex.colorize(nowFrozen
                ? "&#9CFF9CFroze &#FFFFFF" + target.getName() + "&#9CFF9C."
                : "&#9CFF9CUnfroze &#FFFFFF" + target.getName() + "&#9CFF9C."));
        target.sendMessage(MythicHex.colorize(nowFrozen
                ? "&#FF8A8AYou have been frozen by staff."
                : "&#9CFF9CYou have been unfrozen."));
    }
}

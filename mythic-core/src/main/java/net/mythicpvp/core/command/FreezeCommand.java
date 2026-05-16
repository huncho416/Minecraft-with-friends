package net.mythicpvp.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
        sendFreezeNotification(target, nowFrozen);
    }

    public static void sendFreezeNotification(@NotNull Player target, boolean nowFrozen) {
        if (!nowFrozen) {
            target.sendMessage(MythicHex.colorize("&#9CFF9CYou have been unfrozen."));
            return;
        }
        target.sendMessage(Component.empty());
        target.sendMessage(MythicHex.colorize("&#FF8A8A&l&m                                                  "));
        target.sendMessage(MythicHex.colorize("&#FF8A8A&lYOU HAVE BEEN FROZEN BY STAFF"));
        target.sendMessage(Component.empty());
        target.sendMessage(MythicHex.colorize(
                "&#FFFFFFYou must join our Discord within &#FF8A8A3 minutes&#FFFFFF or you will be punished."));
        Component link = MythicHex.colorize("&#9CC3FFhttps://discord.gg/mythicpvp")
                .clickEvent(ClickEvent.openUrl("https://discord.gg/mythicpvp"))
                .hoverEvent(HoverEvent.showText(MythicHex.colorize("&#9CFF9CClick to open Discord")));
        target.sendMessage(MythicHex.colorize("&#FFFFFFDiscord: ").append(link));
        target.sendMessage(MythicHex.colorize(
                "&#D2D8E0Do not log out. Do not move. Wait for staff instructions."));
        target.sendMessage(MythicHex.colorize("&#FF8A8A&l&m                                                  "));
        target.sendMessage(Component.empty());
    }
}

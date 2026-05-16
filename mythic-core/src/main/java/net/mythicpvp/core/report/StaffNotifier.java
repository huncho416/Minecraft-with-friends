package net.mythicpvp.core.report;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StaffNotifier {

    public static final String REPORT_NOTIFY_PERMISSION = "mythic.core.report.notify";
    public static final String HELPOP_NOTIFY_PERMISSION = "mythic.core.helpop.notify";

    private StaffNotifier() {
    }

    public static void notifyReport(@NotNull Report report) {
        Component msg = MythicHex.colorize(
                "&#FF8A8A[REPORT] &#FFFFFF" + report.reporterName()
                        + " &#FF8A8Areported &#FFFFFF" + report.targetName()
                        + " &#FF8A8Afor &#FFFFFF" + report.category().displayName()
                        + " &8(report #" + report.id() + ")");
        broadcast(REPORT_NOTIFY_PERMISSION, msg);
    }

    public static void notifyHelpop(@NotNull Player sender,
                                    @NotNull String serverId,
                                    @NotNull String message) {
        Component nameClickable = MythicHex.colorize("&#FFFFFF" + sender.getName())
                .hoverEvent(HoverEvent.showText(MythicHex.colorize(
                        "&#9CFF9CClick to teleport to &#FFFFFF" + sender.getName())))
                .clickEvent(ClickEvent.runCommand("/tp " + sender.getName()));
        Component header = MythicHex.colorize("&#9CFF9C[HELPOP] ");
        Component server = MythicHex.colorize(" &8(" + serverId + ") &7» &#FFFFFF");
        Component body = MythicHex.colorize(message);
        Component composed = header.append(nameClickable).append(server).append(body);
        broadcast(HELPOP_NOTIFY_PERMISSION, composed);
    }

    private static void broadcast(@NotNull String permission, @NotNull Component message) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(permission)) {
                viewer.sendMessage(message);
            }
        }
        CommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(message);
    }

}

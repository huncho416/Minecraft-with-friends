package net.mythicpvp.core.command;

import net.mythicpvp.core.security.IpTracker;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("ipreport|dupeip")
@CommandPermission("mythic.core.ipreport")
public final class IpReportCommand extends MythicCommand {

    private final IpTracker ipTracker;

    public IpReportCommand(@NotNull IpTracker ipTracker) {
        this.ipTracker = ipTracker;
    }

    @Default
    public void execute(@NotNull CommandSender sender) {
        List<IpTracker.DupeReport> reports = ipTracker.duplicates();
        if (reports.isEmpty()) {
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CNo duplicate-IP players recorded."));
            return;
        }
        sender.sendMessage(MythicHex.colorize(
                "&#F529BEPossible ban evaders / alt accounts &7(&f" + reports.size() + " IP(s)&7):"));
        int shown = 0;
        for (IpTracker.DupeReport report : reports) {
            if (shown >= 20) {
                sender.sendMessage(MythicHex.colorize(
                        "&7…and &f" + (reports.size() - shown) + " &7more — refine with /alts <name>."));
                break;
            }
            StringBuilder line = new StringBuilder("&8• &#FFFFFF").append(report.ip())
                    .append(" &7→ ");
            for (int i = 0; i < report.players().size(); i++) {
                if (i > 0) line.append("&7, ");
                line.append("&f").append(report.players().get(i).name);
            }
            sender.sendMessage(MythicHex.colorize(line.toString()));
            shown++;
        }
    }
}

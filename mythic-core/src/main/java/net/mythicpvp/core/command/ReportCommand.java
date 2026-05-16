package net.mythicpvp.core.command;

import net.mythicpvp.core.report.Report;
import net.mythicpvp.core.report.ReportMenuService;
import net.mythicpvp.core.report.ReportService;
import net.mythicpvp.core.report.StaffNotifier;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.cooldown.CooldownManager;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CommandAlias("report")
public final class ReportCommand extends MythicCommand {

    private final ReportService reportService;
    private final ReportMenuService menuService;
    private final ReportConfig config;
    private final String localShardId;

    public ReportCommand(@NotNull ReportService reportService,
                         @NotNull ReportMenuService menuService,
                         @NotNull ReportConfig config,
                         @NotNull String localShardId) {
        this.reportService = reportService;
        this.menuService = menuService;
        this.config = config;
        this.localShardId = localShardId;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player player, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            sendUsage(player);
            return;
        }
        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou cannot report yourself."));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = target.getUniqueId();
        String cooldownKey = cooldownKey(targetUuid);
        CooldownManager cd = CooldownManager.getInstance();
        if (cd.isOnCooldown(player.getUniqueId(), cooldownKey)) {
            long secs = (cd.getRemainingMillis(player.getUniqueId(), cooldownKey) + 999) / 1000;
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou already reported &#FFFFFF" + targetName
                            + "&#FF8A8A recently. Try again in &#FFFFFF" + secs + "s&#FF8A8A."));
            return;
        }
        String resolvedTargetName = target.getName() == null ? targetName : target.getName();
        menuService.openCategoryPicker(player, resolvedTargetName, targetUuid, (category, finalName) -> {
            if (cd.isOnCooldown(player.getUniqueId(), cooldownKey)) {
                return;
            }
            Report report = reportService.submit(
                    player.getUniqueId(),
                    player.getName(),
                    targetUuid,
                    finalName,
                    category,
                    localShardId);
            cd.set(player.getUniqueId(), cooldownKey, config.cooldownSeconds(), TimeUnit.SECONDS);
            player.sendMessage(MythicHex.colorize(
                    "&#9CFF9CReport submitted against &#FFFFFF" + finalName
                            + "&#9CFF9C for &#FFFFFF" + category.displayName()
                            + "&#9CFF9C. Staff have been notified."));
            StaffNotifier.notifyReport(report);
        });
    }

    private void sendUsage(@NotNull Player player) {
        player.sendMessage(MythicHex.colorize("&#F529BEReport &7System"));
        player.sendMessage(MythicHex.colorize("&7Use &#FFFFFF/report <player>&7 to open the report menu."));
        player.sendMessage(MythicHex.colorize("&7Pick a category in the menu to send the report to staff."));
        player.sendMessage(MythicHex.colorize("&7Cooldown: &f" + config.cooldownSeconds()
                + "s &7per &funique &7player you report."));
        player.sendMessage(MythicHex.colorize("&7False reports are themselves a punishable offense."));
    }

    @NotNull
    private static String cooldownKey(@NotNull UUID target) {
        return "report:" + target;
    }
}

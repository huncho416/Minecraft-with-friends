package net.mythicpvp.core.command;

import net.mythicpvp.core.report.ReportMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("reports")
@CommandPermission("mythic.core.report.review")
public final class ReportsCommand extends MythicCommand {

    private final ReportMenuService menuService;

    public ReportsCommand(@NotNull ReportMenuService menuService) {
        this.menuService = menuService;
    }

    @Default
    public void execute(@NotNull Player player) {
        menuService.openOverview(player);
    }
}

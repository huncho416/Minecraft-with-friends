package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentMenuService;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/history <player>&#888888 - opens punishment history menu.")
@CommandAlias("history")
@CommandPermission("mythic.core.punish.history")
public final class HistoryCommand extends MythicCommand {

    private final PunishmentService punishmentService;
    private final PunishmentMenuService menuService;

    public HistoryCommand(@NotNull PunishmentService punishmentService, @NotNull PunishmentMenuService menuService) {
        this.punishmentService = punishmentService;
        this.menuService = menuService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player player, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        menuService.openHistory(player, targetName, punishmentService.history(target.getUniqueId()));
    }
}

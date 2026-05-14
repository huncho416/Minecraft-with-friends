package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentFlow;
import net.mythicpvp.core.punishment.PunishmentMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("punish")
@CommandPermission("mythic.core.punish.menu")
public final class PunishCommand extends MythicCommand {

    private final PunishmentMenuService menuService;

    public PunishCommand(@NotNull PunishmentMenuService menuService) {
        this.menuService = menuService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player player, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        menuService.openPunish(player, new PunishmentFlow(target.getUniqueId(), targetName, null, "", false, false));
    }
}

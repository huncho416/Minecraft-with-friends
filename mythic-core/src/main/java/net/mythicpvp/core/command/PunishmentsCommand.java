package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("punishments")
@CommandPermission("mythic.core.punish.handbook")
public final class PunishmentsCommand extends MythicCommand {

    private final PunishmentMenuService menuService;

    public PunishmentsCommand(@NotNull PunishmentMenuService menuService) {
        this.menuService = menuService;
    }

    @Default
    public void execute(@NotNull Player player) {
        menuService.openHandbook(player);
    }
}

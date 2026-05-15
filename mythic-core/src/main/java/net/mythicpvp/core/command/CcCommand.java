package net.mythicpvp.core.command;

import net.mythicpvp.core.chat.ChatColorMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("cc")
@CommandPermission("mythic.core.chatcolor")
public final class CcCommand extends MythicCommand {

    private final ChatColorMenuService menuService;

    public CcCommand(@NotNull ChatColorMenuService menuService) {
        this.menuService = menuService;
    }

    @Default
    public void execute(@NotNull Player player) {
        menuService.open(player);
    }
}

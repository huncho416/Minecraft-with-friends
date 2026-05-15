package net.mythicpvp.hub.command;

import net.mythicpvp.hub.selector.ServerSelectorMenu;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("servers|server")
public final class ServerSelectorCommand extends MythicCommand {

    private final ServerSelectorMenu selectorMenu;

    public ServerSelectorCommand(@NotNull ServerSelectorMenu selectorMenu) {
        this.selectorMenu = selectorMenu;
    }

    @Default
    public void execute(@NotNull Player player) {
        selectorMenu.openGroupMenu(player);
    }
}

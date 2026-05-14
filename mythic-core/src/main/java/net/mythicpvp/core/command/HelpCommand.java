package net.mythicpvp.core.command;

import net.mythicpvp.core.essentials.CoreEssentialsService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("help")
public final class HelpCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public HelpCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    public void execute(@NotNull CommandSender sender) {
        essentialsService.sendHelp(sender);
    }
}

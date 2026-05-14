package net.mythicpvp.core.command;

import net.mythicpvp.core.essentials.CoreEssentialsService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("discord")
public final class DiscordCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public DiscordCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    public void execute(@NotNull CommandSender sender) {
        essentialsService.sendDiscord(sender);
    }
}

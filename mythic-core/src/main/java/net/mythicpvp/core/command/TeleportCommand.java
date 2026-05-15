package net.mythicpvp.core.command;

import net.mythicpvp.core.essentials.CoreEssentialsService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Optional;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/tp <player> [destination]&#888888 - teleport to a player, or move them to another player.")
@CommandAlias("tp|teleport")
@CommandPermission("mythic.core.teleport")
public final class TeleportCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public TeleportCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    @Complete({"players", "teleport-others"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName, @Optional String destinationName) {
        essentialsService.teleport(sender, targetName, destinationName);
    }
}

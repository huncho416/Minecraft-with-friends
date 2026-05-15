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

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/gamemode <survival|creative|adventure|spectator> [player]")
@CommandAlias("gamemode|gm")
@CommandPermission("mythic.core.gamemode")
public final class GamemodeCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public GamemodeCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    @Complete({"gamemodes", "essentials-targets"})
    public void execute(@NotNull CommandSender sender, @NotNull String mode, @Optional String targetName) {
        essentialsService.setGameMode(sender, mode, targetName);
    }
}

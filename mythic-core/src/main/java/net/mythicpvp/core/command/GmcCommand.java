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

@CommandAlias("gmc|gm1|creative")
@CommandPermission("mythic.core.gamemode.creative")
public final class GmcCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public GmcCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    @Complete({"essentials-targets"})
    public void execute(@NotNull CommandSender sender, @Optional String targetName) {
        essentialsService.setGameMode(sender, "creative", targetName);
    }
}

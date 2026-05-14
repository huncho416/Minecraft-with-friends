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

@CommandAlias("gms|gm0|survival")
@CommandPermission("mythic.core.gamemode.survival")
public final class GmsCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public GmsCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    @Complete({"essentials-targets"})
    public void execute(@NotNull CommandSender sender, @Optional String targetName) {
        essentialsService.setGameMode(sender, "survival", targetName);
    }
}

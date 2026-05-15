package net.mythicpvp.core.command;

import net.mythicpvp.core.essentials.CoreEssentialsService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/tphere <player>&#888888 - teleport another player to your location.")
@CommandAlias("tphere|tpme")
@CommandPermission("mythic.core.teleport.here")
public final class TpHereCommand extends MythicCommand {

    private final CoreEssentialsService essentialsService;

    public TpHereCommand(@NotNull CoreEssentialsService essentialsService) {
        this.essentialsService = essentialsService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName) {
        essentialsService.teleportHere(sender, targetName);
    }
}

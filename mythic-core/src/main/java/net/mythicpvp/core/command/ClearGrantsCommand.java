package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/cleargrants <player>&#888888 - removes ALL rank grants for a player.")
@CommandAlias("cleargrants")
@CommandPermission("mythic.core.grant.clear")
public final class ClearGrantsCommand extends MythicCommand {

    private final GrantService grantService;

    public ClearGrantsCommand(@NotNull GrantService grantService) {
        this.grantService = grantService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        int removed = grantService.clear(target.getUniqueId());
        sender.sendMessage("Cleared " + removed + " grants for " + targetName + ".");
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.GrantFlow;
import net.mythicpvp.core.rank.GrantFlowService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("grant")
@CommandPermission("mythic.core.grant.menu")
public final class GrantCommand extends MythicCommand {

    private final GrantFlowService grantFlowService;

    public GrantCommand(@NotNull GrantFlowService grantFlowService) {
        this.grantFlowService = grantFlowService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player player, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        grantFlowService.openRankSelection(player, new GrantFlow(target.getUniqueId(), targetName, null, null, null));
    }
}

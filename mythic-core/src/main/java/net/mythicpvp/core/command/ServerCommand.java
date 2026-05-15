package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/server <shard-id>&#888888 - admin: transfer to a specific backend (e.g. /server skyblock-1).")
@CommandAlias("server")
@CommandPermission("mythic.core.server.transfer")
public final class ServerCommand extends MythicCommand {

    private final ProxyTransferService transferService;
    private final CoreMessages messages;

    public ServerCommand(@NotNull ProxyTransferService transferService, @NotNull CoreMessages messages) {
        this.transferService = transferService;
        this.messages = messages;
    }

    @Default
    public void execute(@NotNull Player player, @NotNull String shardId) {
        boolean ok = transferService.transfer(player, shardId);
        if (ok) {
            player.sendMessage(messages.component(
                    "messages.server.transferring",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CTransferring to &#FFFFFF%shard%&#9CFF9C...",
                    Map.of("shard", shardId)));
        } else {
            player.sendMessage(messages.component(
                    "messages.server.transfer-failed",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8ATransfer to %shard% failed.",
                    Map.of("shard", shardId)));
        }
    }
}

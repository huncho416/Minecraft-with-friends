package net.mythicpvp.core.command;

import net.mythicpvp.core.session.CrossShardPresenceService;
import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/send <player> <shard-id>")
@CommandAlias("send")
@CommandPermission("mythic.core.server.transfer")
public final class SendCommand extends MythicCommand {

    private final ProxyTransferService transferService;
    private final CrossShardPresenceService presence;

    public SendCommand(@NotNull ProxyTransferService transferService,
                       @NotNull CrossShardPresenceService presence) {
        this.transferService = transferService;
        this.presence = presence;
    }

    @Default
    @Complete({"players", "shards"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName, @NotNull String shardId) {
        if (sender instanceof Player player && player.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AYou cannot send yourself."));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null && target.isOnline()) {
            if (transferService.transfer(target, shardId)) {
                sender.sendMessage(MythicHex.colorize(
                        "&#9CFF9CSent &#FFFFFF" + target.getName() + " &#9CFF9Cto &#FFFFFF" + shardId + "&#9CFF9C."));
                target.sendMessage(MythicHex.colorize(
                        "&#9CFF9CYou are being sent to &#FFFFFF" + shardId + "&#9CFF9C."));
            } else {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8ATransfer of &#FFFFFF" + target.getName() + " &#FF8A8Afailed."));
            }
            return;
        }
        String remoteShard = presence.shardOf(targetName);
        if (remoteShard != null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FFEC8A" + targetName + " &7is on &#FFFFFF" + remoteShard
                            + "&7. Use &f/server " + remoteShard + " &7then &f/send " + targetName
                            + " " + shardId + "&7."));
        } else {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8APlayer not online: &#FFFFFF" + targetName));
        }
    }
}

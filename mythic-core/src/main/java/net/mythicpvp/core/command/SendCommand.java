package net.mythicpvp.core.command;

import net.mythicpvp.core.session.CrossShardPresenceService;
import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.core.transfer.TransferRequestService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/send <player> <shard-id>")
@CommandAlias("send")
@CommandPermission("mythic.core.server.transfer")
public final class SendCommand extends MythicCommand {

    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    private final ProxyTransferService transferService;
    private final CrossShardPresenceService presence;
    private final TransferRequestService dispatch;

    public SendCommand(@NotNull ProxyTransferService transferService,
                       @NotNull CrossShardPresenceService presence,
                       @NotNull TransferRequestService dispatch) {
        this.transferService = transferService;
        this.presence = presence;
        this.dispatch = dispatch;
    }

    @Default
    @Complete({"players", "shards"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName, @NotNull String shardId) {
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
        if (remoteShard == null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8APlayer not online: &#FFFFFF" + targetName));
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offline.getUniqueId();
        UUID requesterUuid = sender instanceof Player p ? p.getUniqueId() : CONSOLE_UUID;
        if (dispatch.dispatch(targetUuid, targetName, shardId, requesterUuid, sender.getName())) {
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CDispatched send request: &#FFFFFF" + targetName
                            + " &#9CFF9C(on &#FFFFFF" + remoteShard + "&#9CFF9C) → &#FFFFFF" + shardId));
        } else {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8ACould not dispatch send request (STDB unavailable)."));
        }
    }
}

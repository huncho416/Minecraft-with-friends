package net.mythicpvp.core.command;

import net.mythicpvp.core.session.CrossShardPresenceService;
import net.mythicpvp.core.transfer.TransferRequestService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/summon <player>")
@CommandAlias("summon")
@CommandPermission("mythic.core.server.transfer")
public final class SummonCommand extends MythicCommand {

    private final CrossShardPresenceService presence;
    private final TransferRequestService dispatch;
    private final String localShardId;

    public SummonCommand(@NotNull CrossShardPresenceService presence,
                         @NotNull TransferRequestService dispatch,
                         @NotNull String localShardId) {
        this.presence = presence;
        this.dispatch = dispatch;
        this.localShardId = localShardId;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player sender, @NotNull String targetName) {
        if (sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AYou cannot summon yourself."));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null && target.isOnline()) {
            target.teleport(sender.getLocation());
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CSummoned &#FFFFFF" + target.getName() + " &#9CFF9Cto your location."));
            target.sendMessage(MythicHex.colorize(
                    "&#9CFF9CYou were summoned by &#FFFFFF" + sender.getName() + "&#9CFF9C."));
            return;
        }
        String remoteShard = presence.shardOf(targetName);
        if (remoteShard == null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8APlayer not online: &#FFFFFF" + targetName));
            return;
        }
        if (remoteShard.equalsIgnoreCase(localShardId)) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FFEC8A" + targetName + " &7is already on this shard."));
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offline.getUniqueId();
        if (dispatch.dispatch(targetUuid, targetName, localShardId, sender.getUniqueId(), sender.getName())) {
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CSummoning &#FFFFFF" + targetName + " &#9CFF9Cfrom &#FFFFFF" + remoteShard
                            + " &#9CFF9C→ &#FFFFFF" + localShardId + "&#9CFF9C..."));
        } else {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8ACould not dispatch summon (STDB unavailable)."));
        }
    }
}

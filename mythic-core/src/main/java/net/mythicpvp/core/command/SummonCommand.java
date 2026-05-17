package net.mythicpvp.core.command;

import net.mythicpvp.core.session.CrossShardPresenceService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/summon <player>")
@CommandAlias("summon")
@CommandPermission("mythic.core.server.transfer")
public final class SummonCommand extends MythicCommand {

    private final CrossShardPresenceService presence;
    private final String localShardId;

    public SummonCommand(@NotNull CrossShardPresenceService presence,
                         @NotNull String localShardId) {
        this.presence = presence;
        this.localShardId = localShardId;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player sender, @NotNull String targetName) {
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
                    "&#FFEC8A" + targetName + " &7is on this shard but hasn't fully spawned in yet."));
            return;
        }
        sender.sendMessage(MythicHex.colorize(
                "&#FFEC8A" + targetName + " &7is on &#FFFFFF" + remoteShard
                        + "&7. Cross-shard summon requires a transfer-requests STDB table "
                        + "(not yet implemented). Use &f/server " + remoteShard
                        + " &7then &f/send " + targetName + " " + localShardId + "&7."));
    }
}

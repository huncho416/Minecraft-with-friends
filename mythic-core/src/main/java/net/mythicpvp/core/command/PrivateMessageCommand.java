package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@net.mythicpvp.suite.command.Usage("&#FF8A8AUsage: &#FFFFFF/msg <player> <message>&#888888 - send a private message.")
@CommandAlias("msg|message|tell|w")
public final class PrivateMessageCommand extends MythicCommand {

    private final RankService ranks;
    private final GrantService grants;
    private final Map<UUID, UUID> replies = new ConcurrentHashMap<>();

    public PrivateMessageCommand(@NotNull RankService ranks, @NotNull GrantService grants) {
        this.ranks = ranks;
        this.grants = grants;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player sender, @NotNull String targetName, @NotNull String[] words) {
        if (words.length == 0) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &f/msg <player> <message>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AThat player is not online."));
            return;
        }
        if (!canInitiateMessage(sender, target)) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou can only message staff after they message you first."));
            return;
        }
        send(sender, target, String.join(" ", words));
    }

    private boolean canInitiateMessage(@NotNull Player sender, @NotNull Player target) {
        CoreRank senderRank = ranks.get(grants.activeRank(sender.getUniqueId()));
        CoreRank targetRank = ranks.get(grants.activeRank(target.getUniqueId()));
        boolean senderIsStaff = senderRank != null && senderRank.staff();
        boolean targetIsStaff = targetRank != null && targetRank.staff();
        if (senderIsStaff) return true;
        if (!targetIsStaff) return true;
        UUID lastReplyTarget = replies.get(sender.getUniqueId());
        return lastReplyTarget != null && lastReplyTarget.equals(target.getUniqueId());
    }

    void send(@NotNull Player sender, @NotNull Player target, @NotNull String message) {
        replies.put(sender.getUniqueId(), target.getUniqueId());
        replies.put(target.getUniqueId(), sender.getUniqueId());

        String senderDisplay = chatDisplay(sender);
        String targetDisplay = chatDisplay(target);
        sender.sendMessage(MythicHex.colorize("&#D2D8E0(To " + targetDisplay + "&#D2D8E0) &f" + message));
        target.sendMessage(MythicHex.colorize("&#D2D8E0(From " + senderDisplay + "&#D2D8E0) &f" + message));
    }

    @NotNull
    private String chatDisplay(@NotNull Player player) {
        CoreRank rank = ranks.get(grants.activeRank(player.getUniqueId()));
        if (rank == null) {
            return "&#D2D8E0" + player.getName();
        }
        String raw = rank.chatFormat()
                .replace("%chat_prefix%", rank.chatPrefix())
                .replace("%prefix%", rank.prefix())
                .replace("%player%", player.getName())
                .replace("%message%", "");
        return MythicHex.normalizeBareHex(raw);
    }

    @CommandAlias("reply|r")
    public static final class Reply extends MythicCommand {
        private final PrivateMessageCommand parent;

        public Reply(@NotNull PrivateMessageCommand parent) {
            this.parent = parent;
        }

        @Default
        public void execute(@NotNull Player sender, @NotNull String[] words) {
            if (words.length == 0) {
                sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &f/reply <message>"));
                return;
            }
            UUID targetUuid = parent.replies.get(sender.getUniqueId());
            Player target = targetUuid == null ? null : Bukkit.getPlayer(targetUuid);
            if (target == null) {
                sender.sendMessage(MythicHex.colorize("&#FF8A8ANo one to reply to."));
                return;
            }
            parent.send(sender, target, String.join(" ", words));
        }
    }
}

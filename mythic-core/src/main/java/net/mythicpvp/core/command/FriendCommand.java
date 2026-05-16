package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.social.FriendRequest;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@CommandAlias("friend|friends")
public final class FriendCommand extends MythicCommand {

    private final SocialService social;
    private final CoreMessages messages;

    public FriendCommand(@NotNull SocialService social, @NotNull CoreMessages messages) {
        this.social = social;
        this.messages = messages;
    }

    @Default
    public void usage(@NotNull Player player) {
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#F529BEFriend Commands"));
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#FFFFFF/friend add <player> &7- send a friend request"));
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#FFFFFF/friend accept <id> &7- accept a pending request"));
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#FFFFFF/friend deny <id> &7- deny a pending request"));
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#FFFFFF/friend remove <player> &7- remove a friend"));
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#FFFFFF/friend list &7- show your friends and online status"));
        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize("&#FFFFFF/friend requests &7- show pending friend requests"));
    }

    @Subcommand("add")
    @Complete({"players"})
    public void add(@NotNull Player player, @NotNull String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.command.player-not-found",
                    "&#FF8A8AThat player is not online."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.friend-self",
                    "&#FF8A8AYou cannot add yourself as a friend."));
            return;
        }
        FriendRequest request = social.requestFriend(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.friend-request-sent",
                "&#9CFF9CFriend request sent to &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", target.getName())));
        target.sendMessage(messages.component(
                "messages.social.friend-request-received",
                "&#FFFFFF%sender% &#9CFF9Csent you a friend request. &#FFFFFF/friend accept %id%",
                Map.of("sender", player.getName(), "id", Long.toString(request.id()))));
    }

    @Subcommand("accept")
    public void accept(@NotNull Player player, long requestId) {
        if (!social.acceptFriend(requestId, player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.friend-request-missing",
                    "&#FF8A8AThat friend request was not found."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.friend-accepted",
                "&#9CFF9CFriend request accepted."));
    }

    @Subcommand("deny")
    public void deny(@NotNull Player player, long requestId) {
        if (!social.denyFriend(requestId, player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.friend-request-missing",
                    "&#FF8A8AThat friend request was not found."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.friend-denied",
                "&#9CFF9CFriend request denied."));
    }

    @Subcommand("remove")
    @Complete({"players"})
    public void remove(@NotNull Player player, @NotNull String targetName) {
        UUID target = resolveOnlineUuid(targetName);
        if (target == null || !social.areFriends(player.getUniqueId(), target)) {
            player.sendMessage(messages.component(
                    "messages.social.friend-not-found",
                    "&#FF8A8AThat player is not on your friends list."));
            return;
        }
        social.removeFriend(player.getUniqueId(), target);
        player.sendMessage(messages.component(
                "messages.social.friend-removed",
                "&#9CFF9CFriend removed."));
    }

    @Subcommand("list")
    public void list(@NotNull Player player) {
        Set<UUID> friends = social.friendsOf(player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.friend-list",
                "&#FFFFFFYou have &#D2D8E0%count% &#FFFFFFfriends.",
                Map.of("count", Integer.toString(friends.size()))));
        for (UUID friendUuid : friends) {
            Player online = Bukkit.getPlayer(friendUuid);
            String name = online != null ? online.getName() : friendUuid.toString().substring(0, 8);
            String status = online != null && online.isOnline() ? "&#9CFF9COnline" : "&#FF8A8AOffline";
            player.sendMessage(messages.component(
                    "messages.social.friend-list-entry",
                    "&#D2D8E0 - &#FFFFFF%name% %status%",
                    Map.of("name", name, "status", status)));
        }
    }

    @Subcommand("requests")
    public void requests(@NotNull Player player) {
        List<FriendRequest> incoming = social.incomingRequests(player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.friend-requests",
                "&#FFFFFFYou have &#D2D8E0%count% &#FFFFFFpending friend requests.",
                Map.of("count", Integer.toString(incoming.size()))));
        for (FriendRequest request : incoming) {
            Player sender = Bukkit.getPlayer(request.from());
            String senderName = sender != null ? sender.getName() : request.from().toString().substring(0, 8);
            player.sendMessage(messages.component(
                    "messages.social.friend-request-entry",
                    "&#D2D8E0 - &#FFFFFF%sender% &#D2D8E0(id: %id%) &#FFFFFF/friend accept %id%",
                    Map.of("sender", senderName, "id", Long.toString(request.id()))));
        }
    }

    private UUID resolveOnlineUuid(@NotNull String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null ? null : player.getUniqueId();
    }
}

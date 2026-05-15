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
        player.sendMessage(messages.component(
                "messages.social.friend-usage",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AUsage: &#FFFFFF/friend <add|accept|deny|remove|list|requests>"));
    }

    @Subcommand("add")
    @Complete({"players"})
    public void add(@NotNull Player player, @NotNull String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.command.player-not-found",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AThat player is not online."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.friend-self",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AYou cannot add yourself as a friend."));
            return;
        }
        FriendRequest request = social.requestFriend(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.friend-request-sent",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CFriend request sent to &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", target.getName())));
        target.sendMessage(messages.component(
                "messages.social.friend-request-received",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFF%sender% &#9CFF9Csent you a friend request. &#FFFFFF/friend accept %id%",
                Map.of("sender", player.getName(), "id", Long.toString(request.id()))));
    }

    @Subcommand("accept")
    public void accept(@NotNull Player player, long requestId) {
        if (!social.acceptFriend(requestId, player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.friend-request-missing",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AThat friend request was not found."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.friend-accepted",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CFriend request accepted."));
    }

    @Subcommand("deny")
    public void deny(@NotNull Player player, long requestId) {
        if (!social.denyFriend(requestId, player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.friend-request-missing",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AThat friend request was not found."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.friend-denied",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CFriend request denied."));
    }

    @Subcommand("remove")
    @Complete({"players"})
    public void remove(@NotNull Player player, @NotNull String targetName) {
        UUID target = resolveOnlineUuid(targetName);
        if (target == null || !social.areFriends(player.getUniqueId(), target)) {
            player.sendMessage(messages.component(
                    "messages.social.friend-not-found",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AThat player is not on your friends list."));
            return;
        }
        social.removeFriend(player.getUniqueId(), target);
        player.sendMessage(messages.component(
                "messages.social.friend-removed",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CFriend removed."));
    }

    @Subcommand("list")
    public void list(@NotNull Player player) {
        Set<UUID> friends = social.friendsOf(player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.friend-list",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFFYou have &#D2D8E0%count% &#FFFFFFfriends.",
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
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFFYou have &#D2D8E0%count% &#FFFFFFpending friend requests.",
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

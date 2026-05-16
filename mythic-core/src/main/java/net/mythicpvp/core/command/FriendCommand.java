package net.mythicpvp.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.mythicpvp.core.social.FriendRequest;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@CommandAlias("friend|friends")
public final class FriendCommand extends MythicCommand {

    private final SocialService social;

    public FriendCommand(@NotNull SocialService social) {
        this.social = social;
    }

    @Default
    public void usage(@NotNull Player player) {
        player.sendMessage(MythicHex.colorize("&#F529BEFriend Commands"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/friend add <player> &7- send a friend request"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/friend accept <player> &7- accept a pending request"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/friend deny <player> &7- deny a pending request"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/friend remove <player> &7- remove a friend"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/friend list &7- show your friends and online status"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/friend requests &7- show pending friend requests"));
    }

    @Subcommand("add")
    @Complete({"players"})
    public void add(@NotNull Player player, @NotNull String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AThat player is not online."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou cannot add yourself as a friend."));
            return;
        }
        if (social.areFriends(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(MythicHex.colorize(
                    "&#FFEC8AYou are already friends with &#FFFFFF" + target.getName() + "&#FFEC8A."));
            return;
        }
        social.requestFriend(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CFriend request sent to &#FFFFFF" + target.getName() + "&#9CFF9C."));
        String acceptCmd = "/friend accept " + player.getName();
        Component invite = MythicHex.colorize("&#FFFFFF" + player.getName()
                + " &#9CFF9Csent you a friend request. &#FFFFFF[Click to accept]")
                .clickEvent(ClickEvent.runCommand(acceptCmd))
                .hoverEvent(HoverEvent.showText(MythicHex.colorize(
                        "&#9CFF9CClick to run &#FFFFFF" + acceptCmd)));
        target.sendMessage(invite);
        target.sendMessage(MythicHex.colorize(
                "&7(Or type &f" + acceptCmd + " &7to accept.)"));
    }

    @Subcommand("accept")
    @Complete({"players"})
    public void accept(@NotNull Player player, @NotNull String fromName) {
        UUID fromUuid = resolveUuid(fromName);
        if (fromUuid == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AUnknown player: &#FFFFFF" + fromName));
            return;
        }
        FriendRequest request = social.findRequestFrom(player.getUniqueId(), fromUuid);
        if (request == null) {
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8ANo pending friend request from &#FFFFFF" + fromName + "&#FF8A8A."));
            return;
        }
        if (!social.acceptFriend(request.id(), player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8ACould not accept that request."));
            return;
        }
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CYou are now friends with &#FFFFFF" + resolveName(fromUuid, fromName) + "&#9CFF9C."));
        Player sender = Bukkit.getPlayer(fromUuid);
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FFFFFF" + player.getName() + " &#9CFF9Caccepted your friend request."));
        }
    }

    @Subcommand("deny")
    @Complete({"players"})
    public void deny(@NotNull Player player, @NotNull String fromName) {
        UUID fromUuid = resolveUuid(fromName);
        if (fromUuid == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AUnknown player: &#FFFFFF" + fromName));
            return;
        }
        FriendRequest request = social.findRequestFrom(player.getUniqueId(), fromUuid);
        if (request == null) {
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8ANo pending friend request from &#FFFFFF" + fromName + "&#FF8A8A."));
            return;
        }
        social.denyFriend(request.id(), player.getUniqueId());
        player.sendMessage(MythicHex.colorize("&#9CFF9CFriend request denied."));
    }

    @Subcommand("remove")
    @Complete({"players"})
    public void remove(@NotNull Player player, @NotNull String targetName) {
        UUID target = resolveUuid(targetName);
        if (target == null || !social.areFriends(player.getUniqueId(), target)) {
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8A" + targetName + " &#FF8A8Ais not on your friends list."));
            return;
        }
        social.removeFriend(player.getUniqueId(), target);
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CRemoved &#FFFFFF" + resolveName(target, targetName) + " &#9CFF9Cfrom your friends."));
    }

    @Subcommand("list")
    public void list(@NotNull Player player) {
        Set<UUID> friends = social.friendsOf(player.getUniqueId());
        player.sendMessage(MythicHex.colorize(
                "&#F529BEYour friends &7(&f" + friends.size() + "&7)"));
        if (friends.isEmpty()) {
            player.sendMessage(MythicHex.colorize(
                    "&8(none yet — use &f/friend add <player> &8to invite someone)"));
            return;
        }
        for (UUID friendUuid : friends) {
            String name = resolveName(friendUuid, null);
            Player online = Bukkit.getPlayer(friendUuid);
            String status = online != null && online.isOnline() ? "&#9CFF9COnline" : "&#FF8A8AOffline";
            player.sendMessage(MythicHex.colorize("&8• &#FFFFFF" + name + " " + status));
        }
    }

    @Subcommand("requests")
    public void requests(@NotNull Player player) {
        List<FriendRequest> incoming = social.incomingRequests(player.getUniqueId());
        player.sendMessage(MythicHex.colorize(
                "&#F529BEFriend requests &7(&f" + incoming.size() + "&7)"));
        if (incoming.isEmpty()) {
            player.sendMessage(MythicHex.colorize("&8(no pending requests)"));
            return;
        }
        for (FriendRequest request : incoming) {
            String senderName = resolveName(request.from(), null);
            String acceptCmd = "/friend accept " + senderName;
            Component entry = MythicHex.colorize("&8• &#FFFFFF" + senderName
                    + " &7— &#9CFF9C[Click to accept]")
                    .clickEvent(ClickEvent.runCommand(acceptCmd))
                    .hoverEvent(HoverEvent.showText(MythicHex.colorize(
                            "&#9CFF9CClick to run &#FFFFFF" + acceptCmd)));
            player.sendMessage(entry);
        }
    }

    @Nullable
    private static UUID resolveUuid(@NotNull String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.getUniqueId();
    }

    @NotNull
    private static String resolveName(@NotNull UUID uuid, @Nullable String fallback) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        String off = Bukkit.getOfflinePlayer(uuid).getName();
        if (off != null) return off;
        return fallback != null ? fallback : uuid.toString().substring(0, 8);
    }
}

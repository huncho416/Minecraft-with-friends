package net.mythicpvp.core.social;

import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FriendLoginListener implements Listener {

    private final SocialService social;
    private final CoreMessages messages;

    public FriendLoginListener(@NotNull SocialService social, @NotNull CoreMessages messages) {
        this.social = social;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        Set<UUID> friends = social.friendsOf(joiner.getUniqueId());
        if (friends.isEmpty()) {
            return;
        }
        int onlineFriendCount = 0;
        for (UUID friendUuid : friends) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend != null && friend.isOnline()) {
                onlineFriendCount++;
                friend.sendMessage(messages.component(
                        "messages.social.friend-online",
                        "&#FFFFFF%player% &#9CFF9C(friend) joined the network.",
                        Map.of("player", joiner.getName())));
            }
        }
        if (onlineFriendCount > 0) {
            String label = onlineFriendCount == 1 ? "friend is" : "friends are";
            joiner.sendMessage(messages.component(
                    "messages.social.friend-online-count",
                    "&#9CFF9C%count% &#FFFFFF" + label + " &#9CFF9Conline. Use &#FFFFFF/friend list &#9CFF9Cto see them.",
                    Map.of("count", Integer.toString(onlineFriendCount))));
        }
    }
}

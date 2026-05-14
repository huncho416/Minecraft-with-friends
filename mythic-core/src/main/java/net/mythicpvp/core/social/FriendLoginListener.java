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
        for (UUID friendUuid : friends) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend != null && friend.isOnline()) {
                friend.sendMessage(messages.component(
                        "messages.social.friend-online",
                        "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFF%player% &#9CFF9Cis now online.",
                        Map.of("player", joiner.getName())));
            }
        }
    }
}

package net.mythicpvp.core.social;

import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class OfflineRewardService implements Listener {

    private final SocialService social;
    private final CoreMessages messages;

    public OfflineRewardService(@NotNull SocialService social, @NotNull CoreMessages messages) {
        this.social = social;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        LoginStreak streak = social.recordLogin(event.getPlayer().getUniqueId());
        if (streak.currentStreak() > 1) {
            event.getPlayer().sendMessage(messages.component(
                    "messages.social.login-streak",
                    "&#9CFF9CLogin streak: &#FFFFFF%streak% &#9CFF9Cdays!",
                    Map.of("streak", Integer.toString(streak.currentStreak()))));
        }
    }
}

package net.mythicpvp.core.social;

import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class MailLoginListener implements Listener {

    private final SocialService social;
    private final CoreMessages messages;

    public MailLoginListener(@NotNull SocialService social, @NotNull CoreMessages messages) {
        this.social = social;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        int unread = social.unread(event.getPlayer().getUniqueId()).size();
        if (unread == 0) {
            return;
        }
        event.getPlayer().sendMessage(messages.component(
                "messages.social.mail-login",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFFYou have &#D2D8E0%count% &#FFFFFFunread mail.",
                Map.of("count", Integer.toString(unread))));
    }
}

package net.mythicpvp.core.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class ChatGuard implements Listener {

    public static final String BYPASS_PERMISSION = "mythic.core.chat.bypass";

    private static final int CLEAR_BLANK_LINES = 100;

    private final JavaPlugin plugin;
    private final ChatControlService chatControl;
    private final PunishmentService punishments;
    private final CoreMessages messages;

    public ChatGuard(@NotNull JavaPlugin plugin,
                     @NotNull ChatControlService chatControl,
                     @NotNull PunishmentService punishments,
                     @NotNull CoreMessages messages) {
        this.plugin = plugin;
        this.chatControl = chatControl;
        this.punishments = punishments;
        this.messages = messages;

        chatControl.onClear(this::scheduleClear);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        boolean punishedMuted = punishments.active(player.getUniqueId()).stream()
                .anyMatch(record -> record.type() == PunishmentType.MUTE || record.type() == PunishmentType.TEMP_MUTE);
        if (punishedMuted) {
            event.setCancelled(true);
            player.sendMessage(messages.component(
                    "messages.punishments.muted",
                    "&#FF8A8AYou are muted."));
            return;
        }
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }
        if (chatControl.muted()) {
            event.setCancelled(true);
            player.sendMessage(messages.component(
                    "messages.chat-control.blocked-muted",
                    "&#FF8A8AChat is currently muted."));
            return;
        }
        long waitMillis = chatControl.registerMessage(player.getUniqueId(), System.currentTimeMillis());
        if (waitMillis > 0) {
            event.setCancelled(true);
            long secondsRemaining = Math.max(1, (waitMillis + 999) / 1000);
            player.sendMessage(messages.component(
                    "messages.chat-control.blocked-slow",
                    "&#FF8A8ASlow mode active. Wait &#FFFFFF%seconds%s &#FF8A8Abefore sending again.",
                    Map.of("seconds", Long.toString(secondsRemaining))));
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        chatControl.forget(event.getPlayer().getUniqueId());
    }

    private void scheduleClear() {

        MythicScheduler.runSync(plugin, this::clearNow);
    }

    private void clearNow() {
        Component blank = Component.text(" ");
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(BYPASS_PERMISSION)) {
                continue;
            }
            for (int i = 0; i < CLEAR_BLANK_LINES; i++) {
                player.sendMessage(blank);
            }
            player.sendMessage(messages.component(
                    "messages.chat-control.cleared",
                    "&#9CFF9CChat has been cleared."));
        }
    }
}

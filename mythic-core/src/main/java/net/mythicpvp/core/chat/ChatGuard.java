package net.mythicpvp.core.chat;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Enforces network-replicated chat-control state:
 *
 * <ul>
 *   <li>{@code mute} — cancels {@link AsyncPlayerChatEvent} unless the
 *       sender holds the bypass permission.
 *   <li>{@code slow <seconds>} — per-player cool-down recorded in
 *       {@link ChatControlService#registerMessage}.
 *   <li>{@code clear} — when {@link ChatControlService} fires its
 *       clear pulse, this listener flushes 100 blank lines into every
 *       online player's chat. Bypass permission opts a player out so
 *       staff don't lose their own context.
 * </ul>
 *
 * <p>Bypass permission: {@value #BYPASS_PERMISSION}.
 *
 * <p>The listener fires at {@link EventPriority#HIGH} so other plugins
 * (filters, tags, etc.) can still see the message at NORMAL — but we
 * cancel before MONITOR observers process it. {@code ignoreCancelled}
 * is true so we don't fight other plugins that already cancelled.
 */
public final class ChatGuard implements Listener {

    public static final String BYPASS_PERMISSION = "mythic.core.chat.bypass";
    /** 100 blank lines is the conventional "wipe" depth — fills any client window. */
    private static final int CLEAR_BLANK_LINES = 100;

    private final JavaPlugin plugin;
    private final ChatControlService chatControl;
    private final CoreMessages messages;

    public ChatGuard(@NotNull JavaPlugin plugin,
                     @NotNull ChatControlService chatControl,
                     @NotNull CoreMessages messages) {
        this.plugin = plugin;
        this.chatControl = chatControl;
        this.messages = messages;
        // Subscribe to "clear chat" pulses. Pulses arrive on the protocol
        // thread; reschedule onto main before sending blank lines.
        chatControl.onClear(this::scheduleClear);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }
        if (chatControl.muted()) {
            event.setCancelled(true);
            player.sendMessage(messages.component(
                    "messages.chat-control.blocked-muted",
                    "&#FF00F8✘ &#FFFFFFChat is currently muted."));
            return;
        }
        long waitMillis = chatControl.registerMessage(player.getUniqueId(), System.currentTimeMillis());
        if (waitMillis > 0) {
            event.setCancelled(true);
            long secondsRemaining = Math.max(1, (waitMillis + 999) / 1000);
            player.sendMessage(messages.component(
                    "messages.chat-control.blocked-slow",
                    "&#FF00F8✘ &#FFFFFFSlow mode active. Wait %seconds%s before sending again.",
                    Map.of("seconds", Long.toString(secondsRemaining))));
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        chatControl.forget(event.getPlayer().getUniqueId());
    }

    /**
     * Schedule the blank-line flood onto the main thread. Sending
     * messages to many players involves Adventure component encoding,
     * which is fine off-main, but iterating online players can race the
     * tick thread; main keeps it simple and consistent.
     */
    private void scheduleClear() {
        // Folia-safe: routes to globalRegionScheduler on Folia, falls
        // back to legacy BukkitScheduler on Paper/Spigot.
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
                    "&#FF00F8Chat &8» &#FFFFFFChat has been cleared."));
        }
    }
}

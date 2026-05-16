package net.mythicpvp.core.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.core.staff.StaffPresenceListener;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ChatGuard implements Listener {

    public static final String BYPASS_PERMISSION = "mythic.core.chat.bypass";

    private static final int CLEAR_BLANK_LINES = 100;

    private final JavaPlugin plugin;
    private final ChatControlService chatControl;
    private final PunishmentService punishments;
    private final CoreMessages messages;
    private final String serverId;
    @Nullable private final ChatFilterService filters;

    public ChatGuard(@NotNull JavaPlugin plugin,
                     @NotNull ChatControlService chatControl,
                     @NotNull PunishmentService punishments,
                     @NotNull CoreMessages messages,
                     @NotNull String serverId,
                     @Nullable ChatFilterService filters) {
        this.plugin = plugin;
        this.chatControl = chatControl;
        this.punishments = punishments;
        this.messages = messages;
        this.serverId = serverId;
        this.filters = filters;

        chatControl.onClear(this::scheduleClear);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        PunishmentRecord activeMute = punishments.active(player.getUniqueId()).stream()
                .filter(record -> record.type() == PunishmentType.MUTE || record.type() == PunishmentType.TEMP_MUTE)
                .findFirst()
                .orElse(null);
        if (activeMute != null) {
            event.setCancelled(true);
            String reason = activeMute.reason().isEmpty() ? "No reason given" : activeMute.reason();
            String remaining = activeMute.expiresAtMillis() <= 0
                    ? "permanent"
                    : formatRemaining(activeMute.expiresAtMillis() - System.currentTimeMillis());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("reason", reason);
            placeholders.put("remaining", remaining);
            player.sendMessage(messages.component(
                    "messages.punishments.muted",
                    "&#FF8A8AYou are muted: &#FFFFFF%reason% &#D2D8E0(%remaining%)",
                    placeholders));
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
            return;
        }
        if (filters != null && !player.hasPermission("mythic.core.chatfilter.bypass")) {
            String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
            ChatFilterEntry matched = filters.matchFirst(plainMessage);
            if (matched != null) {
                event.setCancelled(true);
                ChatFilterAction action = filters.handleOffense(player.getUniqueId(), player.getName(), matched);
                player.sendMessage(MythicHex.colorize("&#FF8A8A" + action.message()));
                notifyStaff(player, matched, plainMessage, action);
            }
        }
    }

    private void notifyStaff(@NotNull Player offender,
                             @NotNull ChatFilterEntry entry,
                             @NotNull String message,
                             @NotNull ChatFilterAction action) {
        Component line = MythicHex.colorize(
                "&#FFEC8A[Filter] &#FFFFFF" + offender.getName()
                        + " &7on &#FFFFFF" + serverId
                        + " &7tripped &#FFFFFF" + entry.title()
                        + " &7(&f" + action.kind().name() + "&7) &8» &7\"" + truncate(message, 80) + "\"");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(line);
            }
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    @NotNull
    private static String truncate(@NotNull String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max - 1) + "…";
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        chatControl.forget(event.getPlayer().getUniqueId());
    }

    private void scheduleClear() {

        MythicScheduler.runSync(plugin, this::clearNow);
    }

    @NotNull
    private static String formatRemaining(long millis) {
        if (millis <= 0) return "expiring";
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (sb.length() == 0 || seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
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

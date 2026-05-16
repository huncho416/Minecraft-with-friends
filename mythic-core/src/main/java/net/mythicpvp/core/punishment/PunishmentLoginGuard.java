package net.mythicpvp.core.punishment;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.persistence.CoreHydrationSink;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public final class PunishmentLoginGuard implements Listener {

    public static final String BYPASS_PERMISSION = "mythic.core.punish.bypass";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneId.of("UTC"));

    private final PunishmentService punishments;
    private final CoreHydrationSink hydrationSink;
    private final CoreMessages messages;

    public PunishmentLoginGuard(
            @NotNull PunishmentService punishments,
            @NotNull CoreHydrationSink hydrationSink,
            @NotNull CoreMessages messages) {
        this.punishments = punishments;
        this.hydrationSink = hydrationSink;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }
        UUID uuid = player.getUniqueId();

        if (hydrationSink.isBlacklisted(uuid)) {
            player.kick(messages.component(
                    "messages.punishment.login-blacklisted",
                    "<red>You are blacklisted from this network.\n\n<gray>Appeal at <#9CC3FF><click:open_url:'https://discord.gg/mythicpvp'>discord.gg/mythicpvp</click>"));
            return;
        }

        for (PunishmentRecord record : punishments.active(uuid)) {
            if (!record.type().loginBlocking()) {
                continue;
            }
            player.kick(formatBan(record));
            return;
        }
    }

    @NotNull
    private Component formatBan(@NotNull PunishmentRecord record) {
        String typeLabel = switch (record.type()) {
            case BAN -> "Banned";
            case TEMP_BAN -> "Temporarily Banned";
            case BLACKLIST -> "Blacklisted";
            default -> "Banned";
        };
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String issuedAt = TIME_FORMAT.format(Instant.ofEpochMilli(record.createdAtMillis()));
        String expiry = record.expiresAtMillis() <= 0
                ? "Permanent"
                : TIME_FORMAT.format(Instant.ofEpochMilli(record.expiresAtMillis()));
        String remaining;
        if (record.expiresAtMillis() <= 0) {
            remaining = "never";
        } else {
            long ms = record.expiresAtMillis() - System.currentTimeMillis();
            remaining = ms <= 0 ? "expiring" : formatDuration(ms);
        }
        return messages.component(
                "messages.punishment.login-banned",
                "<red><bold>YOU ARE %type%</bold>\n\n"
                + "<white>Reason: <gray>%reason%\n"
                + "<white>Issued by: <gray>%staff%\n"
                + "<white>Issued at: <gray>%issued_at%\n"
                + "<white>Expires: <gray>%expiry%\n"
                + "<white>Unbanned in: <gray>%remaining%\n\n"
                + "<gray>Appeal at <#9CC3FF><click:open_url:'https://discord.gg/mythicpvp'>discord.gg/mythicpvp</click>",
                Map.of(
                        "type", typeLabel,
                        "reason", reasonText,
                        "staff", record.staffName(),
                        "issued_at", issuedAt,
                        "expiry", expiry,
                        "remaining", remaining));
    }

    @NotNull
    private static String formatDuration(long millis) {
        Duration d = Duration.ofMillis(millis);
        long days = d.toDays();
        long hours = d.minusDays(days).toHours();
        long minutes = d.minusDays(days).minusHours(hours).toMinutes();
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (sb.length() == 0 || minutes > 0) sb.append(minutes).append("m");
        return sb.toString().trim();
    }
}

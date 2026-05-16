package net.mythicpvp.core.punishment;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.persistence.CoreHydrationSink;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public final class PunishmentLoginGuard implements Listener {

    public static final String BYPASS_PERMISSION = "mythic.core.punish.bypass";

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        if (hydrationSink.isBlacklisted(uuid)) {
            Component reason = messages.component(
                    "messages.punishment.login-blacklisted",
                    "&#FF8A8AYou are blacklisted from this network.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, reason);
            return;
        }

        for (PunishmentRecord record : punishments.active(uuid)) {
            if (!record.type().loginBlocking()) {
                continue;
            }
            String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
            String expiry = record.expiresAtMillis() <= 0
                    ? "permanent"
                    : "expires " + java.time.Instant.ofEpochMilli(record.expiresAtMillis());
            Component reason = messages.component(
                    "messages.punishment.login-banned",
                    "&#FF8A8AYou are banned: &#FFFFFF%reason% &#D2D8E0(%expiry%).",
                    Map.of(
                            "reason", reasonText,
                            "expiry", expiry,
                            "type", record.type().name()));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, reason);
            return;
        }
    }
}

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

/**
 * Refuses logins for blacklisted players and players with an active
 * login-blocking punishment (BAN, TEMP_BAN).
 *
 * <p>Fires at {@link EventPriority#HIGH} on {@link AsyncPlayerPreLoginEvent}
 * so the rejection lands before the player connects, with no client-side
 * world download wasted.
 *
 * <p>Data sources:
 * <ul>
 *   <li>{@link CoreHydrationSink#isBlacklisted} — populated from STDB's
 *       {@code punishment_blacklist} table by the Phase 3 hydration tier.
 *   <li>{@link PunishmentService#active} — current punishments, also
 *       hydrated from STDB so all servers see the same answer.
 * </ul>
 *
 * <p>Bypass via {@link #BYPASS_PERMISSION} isn't honored at pre-login
 * because the player object isn't constructed yet — by-name allow-listing
 * via the bypass perm only matters once the player is on the server.
 */
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

        // Check blacklist first — it's a hot Map lookup, cheaper than
        // iterating the punishment list.
        if (hydrationSink.isBlacklisted(uuid)) {
            Component reason = messages.component(
                    "messages.punishment.login-blacklisted",
                    "&#FF00F8✘ &#FFFFFFYou are blacklisted from this network.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, reason);
            return;
        }

        // Then scan for an active login-blocking punishment.
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
                    "&#FF00F8✘ &#FFFFFFYou are banned: %reason% (%expiry%).",
                    Map.of(
                            "reason", reasonText,
                            "expiry", expiry,
                            "type", record.type().name()));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, reason);
            return;
        }
    }
}

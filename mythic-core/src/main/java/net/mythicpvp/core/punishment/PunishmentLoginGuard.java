package net.mythicpvp.core.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.persistence.CoreHydrationSink;
import net.mythicpvp.core.persistence.StdbPersistenceGateway;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.PunishmentRow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public final class PunishmentLoginGuard implements Listener {

    public static final String BYPASS_PERMISSION = "mythic.core.punish.bypass";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneId.of("UTC"));

    private final JavaPlugin plugin;
    private final PunishmentService punishments;
    private final CoreHydrationSink hydrationSink;
    private final CoreMessages messages;

    public PunishmentLoginGuard(
            @NotNull JavaPlugin plugin,
            @NotNull PunishmentService punishments,
            @NotNull CoreHydrationSink hydrationSink,
            @NotNull CoreMessages messages) {
        this.plugin = plugin;
        this.punishments = punishments;
        this.hydrationSink = hydrationSink;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(@NotNull PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission(BYPASS_PERMISSION)) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        Component reason = resolveKickReason(uuid);
        if (reason != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, reason);
        }
    }

    @Nullable
    private Component resolveKickReason(@NotNull UUID uuid) {
        if (hydrationSink.isBlacklisted(uuid)) {
            return messages.component(
                    "messages.punishment.login-blacklisted",
                    "<red>You are blacklisted from this network.\n\n<gray>Appeal at <#9CC3FF><click:open_url:'https://discord.gg/mythicpvp'>discord.gg/mythicpvp</click>");
        }
        for (PunishmentRecord record : punishments.active(uuid)) {
            if (record.type().loginBlocking()) {
                return formatBan(record);
            }
        }
        PunishmentRecord stdb = fetchActiveBanFromStdb(uuid);
        if (stdb != null) {
            punishments.applyRecord(stdb);
            return formatBan(stdb);
        }
        return null;
    }

    @Nullable
    private PunishmentRecord fetchActiveBanFromStdb(@NotNull UUID uuid) {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            return null;
        }
        String body;
        try {
            body = connection.sql(
                    "SELECT * FROM " + TableNames.PUNISHMENTS
                            + " WHERE target_uuid = '" + uuid + "' AND active = true").get(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[login-guard] STDB lookup failed", e);
            return null;
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException e) {
            return null;
        }
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) return null;
        JsonObject table = root.getAsJsonArray().get(0).getAsJsonObject();
        if (!table.has("rows")) return null;
        JsonArray rows = table.getAsJsonArray("rows");
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonArray()) continue;
            PunishmentRow row = StdbRowParser.parse(rowElement.toString(), PunishmentRow.class);
            if (row == null) continue;
            PunishmentRecord record = StdbPersistenceGateway.toPunishmentRecord(row);
            if (record.type().loginBlocking() && record.active(System.currentTimeMillis())) {
                return record;
            }
        }
        return null;
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

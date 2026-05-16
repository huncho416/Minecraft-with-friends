package net.mythicpvp.core.punishment;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PunishmentEnforcer implements Consumer<PunishmentNotice> {

    private final JavaPlugin plugin;
    private final CoreMessages messages;

    public PunishmentEnforcer(@NotNull JavaPlugin plugin, @NotNull CoreMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public void accept(@NotNull PunishmentNotice notice) {
        MythicScheduler.runSync(plugin, () -> apply(notice));
    }

    private void apply(@NotNull PunishmentNotice notice) {
        PunishmentRecord record = notice.record();
        Player target = Bukkit.getPlayer(record.targetUuid());

        if (notice.publicBroadcast()) {
            broadcast(record);
        } else {
            notifyStaffSilent(record);
        }

        if (target != null && target.isOnline()) {
            switch (record.type()) {
                case KICK -> kick(target, record);
                case BAN, TEMP_BAN, BLACKLIST -> kick(target, record);
                case MUTE, TEMP_MUTE -> notifyMuted(target, record);
                case WARN -> notifyWarned(target, record);
            }
        }
    }

    private void notifyStaffSilent(@NotNull PunishmentRecord record) {
        String typeLabel = displayType(record.type());
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String duration = record.expiresAtMillis() <= 0
                ? "permanent"
                : formatRemaining(record.expiresAtMillis() - record.createdAtMillis());
        Component line = MythicHex.colorize(
                "&#FFEC8A[Silent] &#FFFFFF" + record.targetName()
                        + " &7was &#FF8A8A" + typeLabel
                        + " &7by &#FFFFFF" + record.staffName()
                        + " &7for &#FFFFFF" + reasonText
                        + " &7(&f" + duration + "&7)");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(net.mythicpvp.core.staff.StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(line);
            }
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    private void kick(@NotNull Player target, @NotNull PunishmentRecord record) {
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String typeLabel = displayType(record.type());
        Component kickReason;
        if (record.type() == PunishmentType.KICK) {
            kickReason = messages.component(
                    "messages.punishment.kicked",
                    "&#FF8A8AYou were kicked: &#FFFFFF%reason%",
                    Map.of("reason", reasonText));
        } else {
            String expiry = record.expiresAtMillis() <= 0
                    ? "permanent"
                    : "until " + java.time.Instant.ofEpochMilli(record.expiresAtMillis());
            kickReason = messages.component(
                    "messages.punishment.banned-kick",
                    "&#FF8A8AYou were %type%: &#FFFFFF%reason% &#D2D8E0(%expiry%)",
                    Map.of(
                            "type", typeLabel,
                            "reason", reasonText,
                            "expiry", expiry));
        }
        target.kick(kickReason);
    }

    private void notifyMuted(@NotNull Player target, @NotNull PunishmentRecord record) {
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String remaining = record.expiresAtMillis() <= 0
                ? "permanent"
                : formatRemaining(record.expiresAtMillis() - System.currentTimeMillis());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", reasonText);
        placeholders.put("remaining", remaining);
        target.sendMessage(messages.component(
                "messages.punishment.notify-muted",
                "&#FF8A8AYou were muted: &#FFFFFF%reason% &#D2D8E0(%remaining%)",
                placeholders));
    }

    public void onPardon(@NotNull PunishmentRecord record) {
        MythicScheduler.runSync(plugin, () -> {
            Player target = Bukkit.getPlayer(record.targetUuid());
            if (target == null || !target.isOnline()) return;
            if (record.type() == PunishmentType.MUTE || record.type() == PunishmentType.TEMP_MUTE) {
                target.sendMessage(messages.component(
                        "messages.punishment.notify-unmuted-staff",
                        "&#9CFF9CYou have been unmuted by staff."));
            } else if (record.type().loginBlocking()) {
                target.sendMessage(messages.component(
                        "messages.punishment.notify-unbanned-staff",
                        "&#9CFF9CYour ban has been lifted."));
            }
        });
    }

    public void onExpiry(@NotNull PunishmentRecord record) {
        MythicScheduler.runSync(plugin, () -> {
            Player target = Bukkit.getPlayer(record.targetUuid());
            if (target == null || !target.isOnline()) return;
            if (record.type() == PunishmentType.TEMP_MUTE) {
                target.sendMessage(messages.component(
                        "messages.punishment.notify-unmuted-expired",
                        "&#9CFF9CYour mute has expired. You can chat again."));
            } else if (record.type() == PunishmentType.TEMP_BAN) {
                target.sendMessage(messages.component(
                        "messages.punishment.notify-unbanned-expired",
                        "&#9CFF9CYour ban has expired."));
            }
        });
    }

    private void notifyWarned(@NotNull Player target, @NotNull PunishmentRecord record) {
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        target.sendMessage(messages.component(
                "messages.punishment.notify-warned",
                "&#FFEC8AYou received a warning: &#FFFFFF%reason%",
                Map.of("reason", reasonText)));
    }

    private void broadcast(@NotNull PunishmentRecord record) {
        String typeLabel = displayType(record.type());
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String duration = record.expiresAtMillis() <= 0
                ? "permanent"
                : formatRemaining(record.expiresAtMillis() - record.createdAtMillis());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type", typeLabel);
        placeholders.put("target", record.targetName());
        placeholders.put("staff", record.staffName());
        placeholders.put("reason", reasonText);
        placeholders.put("duration", duration);
        Component message = messages.component(
                "messages.punishment.broadcast",
                "&#F529BE[Punishment] &#FFFFFF%target% &#D2D8E0was &#FF8A8A%type% &#D2D8E0by &#FFFFFF%staff% &#D2D8E0for &#FFFFFF%reason% &#D2D8E0(%duration%)",
                placeholders);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(MythicHex.colorize("[Punishment] " + record.targetName()
                + " was " + typeLabel + " by " + record.staffName() + " for " + reasonText + " (" + duration + ")"));
    }

    @NotNull
    private static String displayType(@NotNull PunishmentType type) {
        return switch (type) {
            case BAN -> "banned";
            case TEMP_BAN -> "temp-banned";
            case MUTE -> "muted";
            case TEMP_MUTE -> "temp-muted";
            case BLACKLIST -> "blacklisted";
            case WARN -> "warned";
            case KICK -> "kicked";
        };
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
}

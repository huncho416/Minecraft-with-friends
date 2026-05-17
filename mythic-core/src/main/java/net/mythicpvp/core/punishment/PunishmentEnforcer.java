package net.mythicpvp.core.punishment;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.rank.PlayerNameColor;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PunishmentEnforcer implements Consumer<PunishmentNotice> {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneId.of("UTC"));

    private final JavaPlugin plugin;
    private final CoreMessages messages;
    private final PlayerNameColor nameColor;

    public PunishmentEnforcer(@NotNull JavaPlugin plugin, @NotNull CoreMessages messages, @NotNull PlayerNameColor nameColor) {
        this.plugin = plugin;
        this.messages = messages;
        this.nameColor = nameColor;
    }

    @Override
    public void accept(@NotNull PunishmentNotice notice) {
        MythicScheduler.runSync(plugin, () -> apply(notice));
    }

    public void enforceTargetOnly(@NotNull PunishmentRecord record) {
        MythicScheduler.runSync(plugin, () -> {
            Player target = Bukkit.getPlayer(record.targetUuid());
            if (target == null || !target.isOnline()) return;
            switch (record.type()) {
                case KICK, BAN, TEMP_BAN, BLACKLIST -> kick(target, record);
                case MUTE, TEMP_MUTE -> notifyMuted(target, record);
                case WARN -> notifyWarned(target, record);
            }
        });
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
        Component line = messages.codeOwned(buildPunishmentLine(record, true));
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(net.mythicpvp.core.staff.StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(line);
            }
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    @NotNull
    private String buildPunishmentLine(@NotNull PunishmentRecord record, boolean silent) {
        String typeLabel = displayType(record.type());
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String targetColor = nameColor.colorTag(record.targetUuid());
        String staffColor = nameColor.colorTag(record.staffUuid());
        String prefix = silent
                ? "<#FFEC8A>[Silent]</#FFEC8A> "
                : "<#F529BE>[Punishment]</#F529BE> ";
        StringBuilder body = new StringBuilder(prefix);
        body.append(targetColor).append(escape(record.targetName())).append("</").append(tagName(targetColor)).append("> ")
            .append("<#D2D8E0>was <#FF8A8A>").append(typeLabel).append("</#FF8A8A> by </#D2D8E0>")
            .append(staffColor).append(escape(record.staffName())).append("</").append(tagName(staffColor)).append("> ")
            .append("<#D2D8E0>for <white>").append(escape(reasonText)).append("</white></#D2D8E0>");
        if (record.type() != PunishmentType.KICK && record.type() != PunishmentType.WARN && record.expiresAtMillis() > 0) {
            String duration = formatRemaining(record.expiresAtMillis() - record.createdAtMillis());
            body.append(" <#D2D8E0>(<white>").append(duration).append("</white>)</#D2D8E0>");
        }
        return body.toString();
    }

    @NotNull
    private static String escape(@NotNull String text) {
        return text.replace("<", "\\<").replace(">", "\\>");
    }

    @NotNull
    private static String tagName(@NotNull String openTag) {
        if (openTag.length() >= 2 && openTag.startsWith("<") && openTag.endsWith(">")) {
            return openTag.substring(1, openTag.length() - 1);
        }
        return "gray";
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
                    ? "Permanent"
                    : TIME_FORMAT.format(java.time.Instant.ofEpochMilli(record.expiresAtMillis()));
            String remaining;
            if (record.expiresAtMillis() <= 0) {
                remaining = "never";
            } else {
                long ms = record.expiresAtMillis() - System.currentTimeMillis();
                remaining = ms <= 0 ? "expiring" : formatRemaining(ms);
            }
            kickReason = messages.codeOwned(
                    "<red><bold>YOU ARE %type%</bold>\n\n"
                    + "<white>Reason: <gray>%reason%\n"
                    + "<white>Issued by: <gray>%staff%\n"
                    + "<white>Issued at: <gray>%issued_at%\n"
                    + "<white>Expires: <gray>%expiry%\n"
                    + "<white>Unbanned in: <gray>%remaining%\n\n"
                    + "<white>You can appeal this punishment by joining our Discord:\n"
                    + "<#9CC3FF><underlined>discord.gg/mythicpvp</underlined>",
                    Map.of(
                            "type", typeLabel.toUpperCase(java.util.Locale.ROOT),
                            "reason", reasonText,
                            "staff", record.staffName(),
                            "issued_at", TIME_FORMAT.format(java.time.Instant.ofEpochMilli(record.createdAtMillis())),
                            "expiry", expiry,
                            "remaining", remaining));
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

    public void onPardonNotice(@NotNull PardonNotice notice) {
        MythicScheduler.runSync(plugin, () -> {
            PunishmentRecord record = notice.record();
            String actionLabel = pardonActionLabel(record.type());
            String targetColor = nameColor.colorTag(record.targetUuid());
            String staffColor = nameColor.colorTag(record.staffUuid());
            String prefix = notice.silent()
                    ? "<#FFEC8A>[Silent]</#FFEC8A> "
                    : "<#F529BE>[Punishment]</#F529BE> ";
            String body = prefix
                    + targetColor + escape(record.targetName()) + "</" + tagName(targetColor) + "> "
                    + "<#D2D8E0>was <#9CFF9C>" + actionLabel + "</#9CFF9C> by </#D2D8E0>"
                    + staffColor + escape(notice.staffName()) + "</" + tagName(staffColor) + ">"
                    + "<#D2D8E0>.</#D2D8E0>";
            Component line = messages.codeOwned(body);
            if (notice.silent()) {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer.hasPermission(net.mythicpvp.core.staff.StaffPresenceListener.STAFF_PERMISSION)) {
                        viewer.sendMessage(line);
                    }
                }
            } else {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.sendMessage(line);
                }
            }
            Bukkit.getConsoleSender().sendMessage(line);
        });
    }

    @NotNull
    private static String pardonActionLabel(@NotNull PunishmentType type) {
        return switch (type) {
            case BAN, TEMP_BAN, BLACKLIST -> "unbanned";
            case MUTE, TEMP_MUTE -> "unmuted";
            case WARN -> "had a warning cleared";
            case KICK -> "had a kick cleared";
        };
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
        Component message = messages.codeOwned(buildPunishmentLine(record, false));
        String typeLabel = displayType(record.type());
        String reasonText = record.reason().isEmpty() ? "No reason given" : record.reason();
        String duration = record.expiresAtMillis() <= 0
                ? "permanent"
                : formatRemaining(record.expiresAtMillis() - record.createdAtMillis());
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

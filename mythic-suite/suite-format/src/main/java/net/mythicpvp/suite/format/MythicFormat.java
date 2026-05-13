package net.mythicpvp.suite.format;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public final class MythicFormat {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;
    private static final long TRILLION = 1_000_000_000_000L;
    private static final long QUADRILLION = 1_000_000_000_000_000L;

    private MythicFormat() {}

    @NotNull
    private static String truncate(double value) {
        double truncated = Math.floor(value * 100) / 100.0;
        if (truncated == (long) truncated) {
            return String.valueOf((long) truncated);
        }
        String formatted = String.format("%.2f", truncated);
        if (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    @NotNull
    public static String number(long value) {
        if (value < 0) return "-" + number(-value);

        if (value >= QUADRILLION) return truncate((double) value / QUADRILLION) + "Q";
        if (value >= TRILLION) return truncate((double) value / TRILLION) + "T";
        if (value >= BILLION) return truncate((double) value / BILLION) + "B";
        if (value >= MILLION) return truncate((double) value / MILLION) + "M";
        if (value >= THOUSAND) return truncate((double) value / THOUSAND) + "K";

        return String.valueOf(value);
    }

    @NotNull
    public static String number(double value) {
        return number((long) value);
    }

    @NotNull
    public static String money(long value) {
        return "$" + number(value);
    }

    @NotNull
    public static String commas(long value) {
        return String.format("%,d", value);
    }

    @NotNull
    public static String commasMoney(long value) {
        return "$" + commas(value);
    }

    @NotNull
    public static String decimal(double value, int places) {
        return String.format("%." + places + "f", value);
    }

    @NotNull
    public static String percent(double value) {
        double truncated = Math.floor(value * 10000) / 100.0;
        if (truncated == (long) truncated) {
            return (long) truncated + "%";
        }
        String formatted = String.format("%.2f", truncated);
        if (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted + "%";
    }

    @NotNull
    public static String duration(long millis) {
        if (millis <= 0) return "0s";

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    @NotNull
    public static String duration(@NotNull Duration duration) {
        return duration(duration.toMillis());
    }

    @NotNull
    public static String durationCompact(long millis) {
        if (millis <= 0) return "00:00";

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    @NotNull
    public static String durationWords(long millis) {
        if (millis <= 0) return "now";

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
        if (hours > 0) sb.append(hours).append(hours == 1 ? " hour " : " hours ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        if (seconds > 0 && days == 0) sb.append(seconds).append(seconds == 1 ? " second" : " seconds");

        return sb.toString().trim();
    }

    @NotNull
    public static String timeAgo(long epochMillis) {
        long diff = System.currentTimeMillis() - epochMillis;
        if (diff < 0) return "just now";

        if (diff < 60_000) return "just now";
        if (diff < 3_600_000) return (diff / 60_000) + "m ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
        if (diff < 2_592_000_000L) return (diff / 86_400_000) + "d ago";
        if (diff < 31_536_000_000L) return (diff / 2_592_000_000L) + "mo ago";
        return (diff / 31_536_000_000L) + "y ago";
    }

    @NotNull
    public static String timeAgo(@NotNull Instant instant) {
        return timeAgo(instant.toEpochMilli());
    }

    @NotNull
    public static String date(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(DATE_FORMAT);
    }

    @NotNull
    public static String dateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(DATETIME_FORMAT);
    }

    @NotNull
    public static String time(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    @NotNull
    public static String date(@NotNull Instant instant) {
        return date(instant.toEpochMilli());
    }

    @NotNull
    public static String dateTime(@NotNull Instant instant) {
        return dateTime(instant.toEpochMilli());
    }

    public static long parseTime(@NotNull String input) {
        input = input.trim().toLowerCase();
        long multiplier;

        if (input.endsWith("s")) multiplier = 1000L;
        else if (input.endsWith("m")) multiplier = 60_000L;
        else if (input.endsWith("h")) multiplier = 3_600_000L;
        else if (input.endsWith("d")) multiplier = 86_400_000L;
        else if (input.endsWith("w")) multiplier = 604_800_000L;
        else throw new IllegalArgumentException("Unknown time unit: " + input);

        double amount = Double.parseDouble(input.substring(0, input.length() - 1));
        return (long) (amount * multiplier);
    }
}

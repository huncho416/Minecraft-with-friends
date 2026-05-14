package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public record PunishmentTemplate(
        @NotNull PunishmentCategory category,
        @NotNull String duration,
        @NotNull String title,
        @NotNull String information
) {
    public Instant expiresAt(@NotNull Instant now) {
        Duration parsed = parseDuration(duration);
        return parsed == null ? null : now.plus(parsed);
    }

    @NotNull
    public PunishmentType type() {
        return switch (category) {
            case WARN -> PunishmentType.WARN;
            case BLACKLIST -> PunishmentType.BLACKLIST;
            case BAN -> permanent() ? PunishmentType.BAN : PunishmentType.TEMP_BAN;
            case MUTE -> permanent() ? PunishmentType.MUTE : PunishmentType.TEMP_MUTE;
        };
    }

    public boolean permanent() {
        String normalized = duration.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("forever");
    }

    private static Duration parseDuration(@NotNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("forever")) {
            return null;
        }
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("duration");
        }
        long amount = Long.parseLong(normalized.substring(0, normalized.length() - 1));
        return switch (normalized.charAt(normalized.length() - 1)) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("duration");
        };
    }
}

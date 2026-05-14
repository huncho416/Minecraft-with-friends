package net.mythicpvp.core.rank;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Locale;

public record GrantDuration(Duration duration, boolean permanent, @NotNull String input) {

    @NotNull
    public static GrantDuration parse(@NotNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("forever")) {
            return new GrantDuration(null, true, input);
        }
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("duration");
        }
        long amount = Long.parseLong(normalized.substring(0, normalized.length() - 1));
        Duration duration = switch (normalized.charAt(normalized.length() - 1)) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("duration");
        };
        return new GrantDuration(duration, false, input);
    }

    public long expiresAt(long nowMillis) {
        return permanent ? 0L : nowMillis + duration.toMillis();
    }
}

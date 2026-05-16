package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record PunishmentCommand(
        @NotNull String targetName,
        Duration duration,
        @NotNull String reason,
        boolean silent
) {
    @NotNull
    public static PunishmentCommand parse(@NotNull String[] args, boolean durationRequired, @NotNull String defaultReason) {
        List<String> tokens = Arrays.stream(args)
                .filter(token -> !token.isBlank())
                .toList();
        boolean silent = tokens.stream().anyMatch(token -> token.equalsIgnoreCase("-s"));
        List<String> cleaned = tokens.stream()
                .filter(token -> !token.equalsIgnoreCase("-s"))
                .toList();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("target");
        }
        String target = cleaned.getFirst();
        Duration duration = null;
        int reasonStart = 1;
        if (durationRequired) {
            if (cleaned.size() < 2) {
                throw new IllegalArgumentException("duration");
            }
            duration = parseDuration(cleaned.get(1));
            reasonStart = 2;
        }
        String reason = cleaned.size() > reasonStart ? String.join(" ", cleaned.subList(reasonStart, cleaned.size())) : defaultReason;
        return new PunishmentCommand(target, duration, reason, silent);
    }

    public Instant expiresAt(@NotNull Instant now) {
        return duration == null ? null : now.plus(duration);
    }

    @NotNull
    private static Duration parseDuration(@NotNull String input) {
        if (input.length() < 2) {
            throw new IllegalArgumentException("duration");
        }
        Duration total = Duration.ZERO;
        StringBuilder digits = new StringBuilder();
        boolean parsedAny = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
                continue;
            }
            if (digits.length() == 0) {
                throw new IllegalArgumentException("duration");
            }
            long amount = Long.parseLong(digits.toString());
            digits.setLength(0);
            parsedAny = true;
            total = total.plus(switch (Character.toLowerCase(c)) {
                case 's' -> Duration.ofSeconds(amount);
                case 'm' -> Duration.ofMinutes(amount);
                case 'h' -> Duration.ofHours(amount);
                case 'd' -> Duration.ofDays(amount);
                case 'w' -> Duration.ofDays(amount * 7L);
                default -> throw new IllegalArgumentException("duration");
            });
        }
        if (!parsedAny || digits.length() > 0) {
            throw new IllegalArgumentException("duration");
        }
        return total;
    }
}

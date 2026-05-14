package net.mythicpvp.core.rank;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RankGrant(
        long id,
        @NotNull UUID targetUuid,
        @NotNull String targetName,
        @NotNull String rankId,
        @NotNull UUID executorUuid,
        @NotNull String executorName,
        @NotNull String reason,
        long createdAtMillis,
        long expiresAtMillis,
        boolean active
) {
    public boolean permanent() {
        return expiresAtMillis <= 0;
    }

    public boolean expired(long nowMillis) {
        return !permanent() && expiresAtMillis <= nowMillis;
    }
}

package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PunishmentRecord(
        long id,
        @NotNull UUID targetUuid,
        @NotNull String targetName,
        @NotNull UUID staffUuid,
        @NotNull String staffName,
        @NotNull PunishmentType type,
        @NotNull String reason,
        @NotNull String proof,
        long createdAtMillis,
        long expiresAtMillis,
        boolean silent,
        boolean clearInventory,
        boolean pardoned,
        @NotNull String server
) {
    public boolean active(long nowMillis) {
        return !pardoned && (expiresAtMillis <= 0 || expiresAtMillis > nowMillis);
    }
}

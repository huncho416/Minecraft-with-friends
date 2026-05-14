package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public record PunishmentRequest(
        @NotNull UUID targetUuid,
        @NotNull String targetName,
        @NotNull UUID staffUuid,
        @NotNull String staffName,
        @NotNull PunishmentType type,
        @NotNull String reason,
        @NotNull String proof,
        Instant expiresAt,
        boolean silent,
        boolean clearInventory,
        @NotNull String server
) {
    public PunishmentRequest(@NotNull UUID targetUuid, @NotNull String targetName, @NotNull UUID staffUuid, @NotNull String staffName, @NotNull PunishmentType type, @NotNull String reason, Instant expiresAt, boolean silent, @NotNull String server) {
        this(targetUuid, targetName, staffUuid, staffName, type, reason, "", expiresAt, silent, false, server);
    }
}

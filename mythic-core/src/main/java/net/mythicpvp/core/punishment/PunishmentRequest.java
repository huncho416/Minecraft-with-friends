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
        Instant expiresAt,
        boolean silent,
        @NotNull String server
) {}

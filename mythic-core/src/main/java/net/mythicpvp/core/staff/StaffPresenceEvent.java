package net.mythicpvp.core.staff;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record StaffPresenceEvent(
        @NotNull StaffPresenceType type,
        @NotNull String server,
        @NotNull UUID staffUuid,
        @NotNull String staffName,
        @NotNull String rank,
        @NotNull String rankColor,
        @NotNull String fromServer,
        @NotNull String toServer,
        long createdAtMillis
) {}

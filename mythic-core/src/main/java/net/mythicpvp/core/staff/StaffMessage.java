package net.mythicpvp.core.staff;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record StaffMessage(
        @NotNull StaffChannel channel,
        @NotNull String server,
        @NotNull UUID senderUuid,
        @NotNull String senderName,
        @NotNull String rank,
        @NotNull String rankColor,
        @NotNull String message,
        long createdAtMillis
) {}

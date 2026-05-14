package net.mythicpvp.core.social;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FriendRequest(
        long id,
        @NotNull UUID from,
        @NotNull UUID to,
        long createdAtMillis
) {}

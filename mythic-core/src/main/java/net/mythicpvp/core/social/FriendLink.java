package net.mythicpvp.core.social;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FriendLink(
        long id,
        @NotNull UUID owner,
        @NotNull UUID friend,
        long addedAtMillis
) {}

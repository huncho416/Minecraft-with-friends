package net.mythicpvp.core.social;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record Party(
        long id,
        @NotNull UUID leader,
        long createdAtMillis
) {}

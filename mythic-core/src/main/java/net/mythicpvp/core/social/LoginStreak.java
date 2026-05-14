package net.mythicpvp.core.social;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LoginStreak(
        long id,
        @NotNull UUID player,
        long lastLoginMillis,
        int currentStreak
) {}

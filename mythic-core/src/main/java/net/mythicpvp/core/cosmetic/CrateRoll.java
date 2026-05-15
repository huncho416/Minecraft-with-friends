package net.mythicpvp.core.cosmetic;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CrateRoll(
        @NotNull UUID player,
        @NotNull String crateId,
        @NotNull String cosmeticId,
        double rollPercentage,
        long timestamp) {}

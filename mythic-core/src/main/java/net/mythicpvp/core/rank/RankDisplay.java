package net.mythicpvp.core.rank;

import org.jetbrains.annotations.NotNull;

public record RankDisplay(
        @NotNull String chatPrefix,
        @NotNull String chatFormat,
        @NotNull String tabPrefix,
        @NotNull String tabFormat,
        @NotNull String nametagPrefix,
        @NotNull String nametagFormat
) {}

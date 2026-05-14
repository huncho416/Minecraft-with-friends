package net.mythicpvp.core.rank;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CoreRank(
        @NotNull String id,
        @NotNull String name,
        @NotNull String color,
        @NotNull Material dye,
        @NotNull String prefix,
        @NotNull String suffix,
        int weight,
        boolean staff,
        boolean donator,
        @NotNull String parent,
        @NotNull List<String> permissions,
        @NotNull String chatPrefix,
        @NotNull String chatFormat,
        @NotNull String tabPrefix,
        @NotNull String tabFormat,
        @NotNull String nametagPrefix,
        @NotNull String nametagFormat
) {}

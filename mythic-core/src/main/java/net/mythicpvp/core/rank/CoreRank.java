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
        @NotNull String nametagFormat,
        @NotNull String scope
) {
    public static final String SCOPE_GLOBAL = "global";

    public boolean matchesNetwork(@NotNull String networkType) {
        return scope.equalsIgnoreCase(SCOPE_GLOBAL) || scope.equalsIgnoreCase(networkType);
    }

    @NotNull
    public CoreRank withScope(@NotNull String newScope) {
        return new CoreRank(id, name, color, dye, prefix, suffix, weight, staff, donator,
                parent, permissions, chatPrefix, chatFormat, tabPrefix, tabFormat,
                nametagPrefix, nametagFormat, newScope.isBlank() ? SCOPE_GLOBAL : newScope);
    }
}

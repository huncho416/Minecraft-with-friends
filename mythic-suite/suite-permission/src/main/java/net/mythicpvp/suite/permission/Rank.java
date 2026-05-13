package net.mythicpvp.suite.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Rank implements Comparable<Rank> {

    private final String name;
    private final String prefix;
    private final String hexColor;
    private final int weight;
    private final Set<String> permissions;
    private final String parent;

    public Rank(@NotNull String name, @NotNull String prefix, @NotNull String hexColor, int weight, @NotNull Set<String> permissions, @Nullable String parent) {
        this.name = name;
        this.prefix = prefix;
        this.hexColor = hexColor;
        this.weight = weight;
        this.permissions = new HashSet<>(permissions);
        this.parent = parent;
    }

    @NotNull public String getName() { return name; }
    @NotNull public String getPrefix() { return prefix; }
    @NotNull public String getHexColor() { return hexColor; }
    public int getWeight() { return weight; }
    @NotNull public Set<String> getPermissions() { return Collections.unmodifiableSet(permissions); }
    @Nullable public String getParent() { return parent; }

    @Override
    public int compareTo(@NotNull Rank other) {
        return Integer.compare(this.weight, other.weight);
    }
}

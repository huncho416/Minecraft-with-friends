package net.mythicpvp.suite.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionManager {

    private static final PermissionManager INSTANCE = new PermissionManager();
    private final Map<String, Rank> ranks = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerRanks = new ConcurrentHashMap<>();

    private PermissionManager() {}

    @NotNull
    public static PermissionManager getInstance() {
        return INSTANCE;
    }

    public void registerRank(@NotNull Rank rank) {
        ranks.put(rank.getName().toLowerCase(), rank);
    }

    @Nullable
    public Rank getRank(@NotNull String name) {
        return ranks.get(name.toLowerCase());
    }

    @NotNull
    public Rank getPlayerRank(@NotNull UUID player) {
        String rankName = playerRanks.getOrDefault(player, "default");
        Rank rank = ranks.get(rankName.toLowerCase());
        return rank != null ? rank : ranks.getOrDefault("default", new Rank("default", "&7", "#808080", 999, Set.of(), null));
    }

    public void setPlayerRank(@NotNull UUID player, @NotNull String rankName) {
        playerRanks.put(player, rankName.toLowerCase());
    }

    public boolean hasPermission(@NotNull UUID player, @NotNull String permission) {
        Rank rank = getPlayerRank(player);
        return hasPermissionRecursive(rank, permission);
    }

    private boolean hasPermissionRecursive(@NotNull Rank rank, @NotNull String permission) {
        if (rank.getPermissions().contains("*")) return true;
        if (rank.getPermissions().contains(permission)) return true;

        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) wildcard.append(".");
            wildcard.append(parts[i]);
            if (rank.getPermissions().contains(wildcard + ".*")) return true;
        }

        if (rank.getParent() != null) {
            Rank parent = ranks.get(rank.getParent().toLowerCase());
            if (parent != null) return hasPermissionRecursive(parent, permission);
        }

        return false;
    }

    @NotNull
    public Collection<Rank> getAllRanks() {
        List<Rank> sorted = new ArrayList<>(ranks.values());
        Collections.sort(sorted);
        return sorted;
    }
}

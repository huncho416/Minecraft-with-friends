package net.mythicpvp.suite.cosmetic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticManager {

    private static final CosmeticManager INSTANCE = new CosmeticManager();
    private final Map<String, Cosmetic> registry = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> owned = new ConcurrentHashMap<>();
    private final Map<String, String> equipped = new ConcurrentHashMap<>();

    private CosmeticManager() {}

    @NotNull
    public static CosmeticManager getInstance() {
        return INSTANCE;
    }

    public void register(@NotNull Cosmetic cosmetic) {
        registry.put(cosmetic.id().toLowerCase(), cosmetic);
    }

    @Nullable
    public Cosmetic get(@NotNull String id) {
        return registry.get(id.toLowerCase());
    }

    @NotNull
    public Collection<Cosmetic> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    @NotNull
    public Collection<Cosmetic> getByType(@NotNull CosmeticType type) {
        return registry.values().stream().filter(c -> c.type() == type).toList();
    }

    public void grantCosmetic(@NotNull UUID player, @NotNull String cosmeticId) {
        owned.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(cosmeticId.toLowerCase());
    }

    public boolean ownsCosmetic(@NotNull UUID player, @NotNull String cosmeticId) {
        Set<String> playerOwned = owned.get(player);
        return playerOwned != null && playerOwned.contains(cosmeticId.toLowerCase());
    }

    @NotNull
    public Set<String> getOwned(@NotNull UUID player) {
        return owned.getOrDefault(player, Collections.emptySet());
    }

    public void equip(@NotNull UUID player, @NotNull CosmeticType type, @NotNull String cosmeticId) {
        equipped.put(player.toString() + ":" + type.name(), cosmeticId.toLowerCase());
    }

    public void unequip(@NotNull UUID player, @NotNull CosmeticType type) {
        equipped.remove(player.toString() + ":" + type.name());
    }

    @Nullable
    public String getEquipped(@NotNull UUID player, @NotNull CosmeticType type) {
        return equipped.get(player.toString() + ":" + type.name());
    }

    public record Cosmetic(@NotNull String id, @NotNull String displayName, @NotNull CosmeticType type,
                           @NotNull String description, @Nullable org.bukkit.NamespacedKey itemModel,
                           @NotNull String rarity, boolean tradable, boolean limited,
                           @Nullable String format) {

        public Cosmetic(@NotNull String id, @NotNull String displayName, @NotNull CosmeticType type,
                        @NotNull String description, @Nullable org.bukkit.NamespacedKey itemModel,
                        @NotNull String rarity, boolean tradable, boolean limited) {
            this(id, displayName, type, description, itemModel, rarity, tradable, limited, null);
        }
    }
}

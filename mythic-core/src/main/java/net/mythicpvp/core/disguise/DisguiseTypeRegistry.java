package net.mythicpvp.core.disguise;

import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DisguiseTypeRegistry {

    public static final String WILDCARD_PERMISSION = "mythic.core.disguise.type.*";

    private final Map<String, DisguiseType> types = new LinkedHashMap<>();

    public DisguiseTypeRegistry() {
        register(new DisguiseType("player", "Player", DisguiseCategory.PLAYER));
    }

    public void register(@NotNull DisguiseType type) {
        types.put(type.id(), type);
    }

    @NotNull
    public Collection<DisguiseType> all() {
        return java.util.Collections.unmodifiableCollection(types.values());
    }

    public boolean canUse(@NotNull Permissible permissible, @NotNull String typeId) {
        if (permissible.hasPermission(WILDCARD_PERMISSION)) return true;
        return permissible.hasPermission("mythic.core.disguise.type." + typeId.toLowerCase());
    }

    public boolean isImplemented(@NotNull String typeId) {
        DisguiseType type = types.get(typeId.toLowerCase());
        return type != null && type.category() == DisguiseCategory.PLAYER;
    }

    public enum DisguiseCategory { PLAYER, MOB, MISC }

    public record DisguiseType(@NotNull String id, @NotNull String displayName, @NotNull DisguiseCategory category) {}
}

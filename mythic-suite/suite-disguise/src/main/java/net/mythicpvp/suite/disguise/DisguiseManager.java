package net.mythicpvp.suite.disguise;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DisguiseManager {

    private static final DisguiseManager INSTANCE = new DisguiseManager();
    private final Map<UUID, DisguiseData> disguises = new ConcurrentHashMap<>();

    private DisguiseManager() {}

    @NotNull
    public static DisguiseManager getInstance() {
        return INSTANCE;
    }

    public void disguise(@NotNull UUID player, @NotNull DisguiseData data) {
        disguises.put(player, data);
    }

    public void undisguise(@NotNull UUID player) {
        disguises.remove(player);
    }

    public boolean isDisguised(@NotNull UUID player) {
        return disguises.containsKey(player);
    }

    @Nullable
    public DisguiseData getDisguise(@NotNull UUID player) {
        return disguises.get(player);
    }

    @NotNull
    public String getDisplayName(@NotNull UUID player, @NotNull String realName) {
        DisguiseData data = disguises.get(player);
        return data != null ? data.displayName() : realName;
    }

    public record DisguiseData(
            @NotNull String displayName,
            @Nullable String skinValue,
            @Nullable String skinSignature,
            @Nullable String rankOverride
    ) {
        @NotNull
        public static DisguiseData withName(@NotNull String name) {
            return new DisguiseData(name, null, null, null);
        }

        @NotNull
        public static DisguiseData full(@NotNull String name, @NotNull String skinValue, @NotNull String skinSignature, @Nullable String rankOverride) {
            return new DisguiseData(name, skinValue, skinSignature, rankOverride);
        }
    }
}

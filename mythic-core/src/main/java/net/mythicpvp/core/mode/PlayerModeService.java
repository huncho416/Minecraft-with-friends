package net.mythicpvp.core.mode;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerModeService {

    private final Set<UUID> builders = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pvpers = ConcurrentHashMap.newKeySet();

    public boolean toggleBuild(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (builders.remove(uuid)) return false;
        builders.add(uuid);
        return true;
    }

    public boolean togglePvp(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (pvpers.remove(uuid)) return false;
        pvpers.add(uuid);
        return true;
    }

    public boolean isBuilder(@NotNull Player player) {
        return builders.contains(player.getUniqueId());
    }

    public boolean isPvp(@NotNull Player player) {
        return pvpers.contains(player.getUniqueId());
    }

    public void clear(@NotNull UUID uuid) {
        builders.remove(uuid);
        pvpers.remove(uuid);
    }
}

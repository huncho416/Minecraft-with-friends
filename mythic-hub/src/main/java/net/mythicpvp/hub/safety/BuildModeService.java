package net.mythicpvp.hub.safety;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildModeService {

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();

    public boolean toggle(@NotNull UUID player) {
        if (active.contains(player)) {
            active.remove(player);
            return false;
        }
        active.add(player);
        return true;
    }

    public boolean isActive(@NotNull UUID player) {
        return active.contains(player);
    }

    public void clear(@NotNull UUID player) {
        active.remove(player);
    }
}

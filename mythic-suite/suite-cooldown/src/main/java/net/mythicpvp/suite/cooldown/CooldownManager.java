package net.mythicpvp.suite.cooldown;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class CooldownManager {

    private static final CooldownManager INSTANCE = new CooldownManager();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    private CooldownManager() {}

    @NotNull
    public static CooldownManager getInstance() {
        return INSTANCE;
    }

    public void set(@NotNull UUID player, @NotNull String name, long duration, @NotNull TimeUnit unit) {
        if (duration < 0) {
            throw new IllegalArgumentException("Cooldown duration cannot be negative");
        }
        String key = player.toString() + ":" + name;
        cooldowns.put(key, System.currentTimeMillis() + unit.toMillis(duration));
    }

    public boolean isOnCooldown(@NotNull UUID player, @NotNull String name) {
        String key = player.toString() + ":" + name;
        Long expiry = cooldowns.get(key);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(key);
            return false;
        }
        return true;
    }

    public long getRemainingMillis(@NotNull UUID player, @NotNull String name) {
        String key = player.toString() + ":" + name;
        Long expiry = cooldowns.get(key);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }
        return remaining;
    }

    public double getRemainingSeconds(@NotNull UUID player, @NotNull String name) {
        return getRemainingMillis(player, name) / 1000.0;
    }

    public void remove(@NotNull UUID player, @NotNull String name) {
        cooldowns.remove(player.toString() + ":" + name);
    }

    public void removeAll(@NotNull UUID player) {
        String prefix = player.toString() + ":";
        cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(e -> e.getValue() <= now);
    }

    public void clear() {
        cooldowns.clear();
    }
}

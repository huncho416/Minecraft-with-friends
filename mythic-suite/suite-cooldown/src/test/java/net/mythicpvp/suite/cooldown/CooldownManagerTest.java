package net.mythicpvp.suite.cooldown;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownManagerTest {

    private final CooldownManager manager = CooldownManager.getInstance();

    @AfterEach
    void cleanup() {
        manager.clear();
    }

    @Test
    void tracksAndClearsCooldowns() {
        UUID player = UUID.randomUUID();
        manager.set(player, "warp", 1, TimeUnit.MINUTES);
        assertTrue(manager.isOnCooldown(player, "warp"));
        manager.remove(player, "warp");
        assertFalse(manager.isOnCooldown(player, "warp"));
    }

    @Test
    void rejectsNegativeDurations() {
        assertThrows(IllegalArgumentException.class, () -> manager.set(UUID.randomUUID(), "warp", -1, TimeUnit.SECONDS));
    }
}

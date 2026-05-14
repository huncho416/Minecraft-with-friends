package net.mythicpvp.hub;

import net.mythicpvp.hub.spawn.SpawnService;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpawnServiceTest {

    private SpawnService service;

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        service = new SpawnService(plugin);
    }

    @Test
    void defaultVoidTeleportY() {
        assertEquals(0.0, service.getVoidTeleportY());
    }

    @Test
    void spawnLocationIsNullBeforeLoad() {
        assertNull(service.getSpawnLocation());
    }
}

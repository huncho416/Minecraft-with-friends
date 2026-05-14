package net.mythicpvp.hub;

import net.mythicpvp.hub.spawn.SpawnService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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

    @Test
    void isBelowVoidReturnsTrueWhenBelowThreshold() {
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(null, 0, -15, 0));
        assertTrue(service.isBelowVoid(player));
    }

    @Test
    void isBelowVoidReturnsFalseWhenAboveThreshold() {
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(null, 0, 50, 0));
        assertFalse(service.isBelowVoid(player));
    }

    @Test
    void teleportToSpawnDoesNothingWhenNoSpawnLoaded() {
        Player player = mock(Player.class);
        service.teleportToSpawn(player);
        verify(player, never()).teleport(any(Location.class));
    }
}

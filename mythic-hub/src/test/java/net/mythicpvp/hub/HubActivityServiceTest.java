package net.mythicpvp.hub;

import net.mythicpvp.hub.activity.HubActivityService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HubActivityServiceTest {

    private HubActivityService service;

    @BeforeEach
    void setUp() {
        service = new HubActivityService();
    }

    @Test
    void doubleJumpDisabledByDefault() {
        assertFalse(service.isDoubleJumpEnabled());
    }

    @Test
    void launchPadsDisabledByDefault() {
        assertFalse(service.isLaunchPadsEnabled());
    }

    @Test
    void enableFlightDoesNothingWhenDisabled() {
        Player player = mock(Player.class);
        service.enableFlight(player);
        verify(player, never()).setAllowFlight(true);
    }

    @Test
    void applyDoubleJumpDoesNothingWhenDisabled() {
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(null, 0, 64, 0, 0, 0));
        service.applyDoubleJump(player);
        verify(player, never()).setVelocity(any(Vector.class));
    }

    @Test
    void applyLaunchPadDoesNothingWhenDisabled() {
        Player player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(null, 0, 64, 0, 0, 0));
        service.applyLaunchPad(player);
        verify(player, never()).setVelocity(any(Vector.class));
    }

    @Test
    void defaultLaunchPadMaterialIsNull() {
        assertNull(service.getLaunchPadMaterial());
    }
}

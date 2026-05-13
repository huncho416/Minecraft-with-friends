package net.mythicpvp.suite.disguise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguiseManagerTest {

    private final DisguiseManager manager = DisguiseManager.getInstance();

    @AfterEach
    void cleanup() {
        manager.clear();
    }

    @Test
    void resolvesDisplayNamesAndStaffVisibility() {
        UUID viewer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        manager.disguiseAs(target, "Nick", new DisguiseManager.SkinProperties("value", "sig"), "vip");
        assertEquals("Nick", manager.getDisplayName(target, "Real"));
        assertEquals("vip", manager.getRankOverride(target));
        manager.setStaffView(viewer, true);
        assertEquals("Nick (Real)", manager.getVisibleName(viewer, target, "Real"));
    }

    @Test
    void createsRandomNickFromConfiguredNames() {
        UUID target = UUID.randomUUID();
        manager.setRandomNames(List.of("OnlyName"));
        assertTrue(manager.disguiseRandom(target).random());
        assertEquals("OnlyName", manager.getDisplayName(target, "Real"));
    }
}

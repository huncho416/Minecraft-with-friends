package net.mythicpvp.suite.skin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinManagerTest {

    private final SkinManager manager = SkinManager.getInstance();

    @AfterEach
    void cleanup() {
        manager.clearCache();
    }

    @Test
    void returnsCachedSkinWithoutNetworkFetch() {
        UUID uuid = UUID.randomUUID();
        SkinManager.SkinData data = new SkinManager.SkinData("value", "signature");
        manager.cache(uuid, data);
        assertEquals(data, manager.fetch(uuid).join());
    }
}

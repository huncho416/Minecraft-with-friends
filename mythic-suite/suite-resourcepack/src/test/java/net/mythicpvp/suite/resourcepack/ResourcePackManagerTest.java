package net.mythicpvp.suite.resourcepack;

import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourcePackManagerTest {

    private final ResourcePackManager manager = ResourcePackManager.getInstance();

    @AfterEach
    void cleanup() {
        manager.clear();
    }

    @Test
    void computesHashAndRegistersModelsAndFonts() throws Exception {
        Path pack = Files.createTempFile("mythic-pack", ".zip");
        Files.writeString(pack, "mythic");
        assertEquals("af1fc7004e01a84ca36b0048155fb7d37d1fc41b", manager.computeHash(pack));
        manager.registerModel("mythic_sword", Material.DIAMOND_SWORD, 10001);
        manager.registerFont("title", "smpd:font/mythic_title");
        assertEquals(10001, manager.getModel("MYTHIC_SWORD").customModelData());
        assertEquals("smpd:font/mythic_title", manager.getFont("TITLE"));
    }

    @Test
    void recordsPackDelivery() {
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        manager.setPackInfo("pack-url", "hash");
        manager.setForceUpdate(true);
        manager.sendTo(player);
        verify(player).sendResourcePacks(any(ResourcePackRequest.class));
        assertTrue(manager.getDelivery(uuid).orElseThrow().forced());
    }
}

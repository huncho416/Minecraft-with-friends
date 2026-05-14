package net.mythicpvp.suite.item;

import net.mythicpvp.suite.resourcepack.ResourcePackManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MythicItemTest {

    @BeforeEach
    void setup() {
        MockBukkit.mock();
    }

    @AfterEach
    void cleanup() {
        ResourcePackManager.getInstance().clear();
        MockBukkit.unmock();
    }

    @Test
    void appliesResourcePackModel() {
        NamespacedKey swordModel = NamespacedKey.fromString("mythic:mythic_sword");
        ResourcePackManager.getInstance().registerModel("mythic_sword", Material.DIAMOND_SWORD, swordModel);

        assertDoesNotThrow(() -> MythicItem.create(Material.DIAMOND_SWORD).model("mythic_sword").build());
        assertThrows(IllegalArgumentException.class, () -> MythicItem.create(Material.STONE).model("mythic_sword"));
        assertThrows(IllegalArgumentException.class, () -> MythicItem.create(Material.DIAMOND_SWORD).model("unknown"));
    }
}

package net.mythicpvp.suite.item;

import net.mythicpvp.suite.resourcepack.ResourcePackManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MythicItemTest {

    @AfterEach
    void cleanup() {
        ResourcePackManager.getInstance().clear();
    }

    @Test
    void appliesResourcePackModel() {
        ResourcePackManager.getInstance().registerModel("mythic_sword", Material.DIAMOND_SWORD, 10001);
        ItemStack item = MythicItem.create(Material.DIAMOND_SWORD).model("mythic_sword").build();
        assertEquals(10001, item.getItemMeta().getCustomModelData());
        assertThrows(IllegalArgumentException.class, () -> MythicItem.create(Material.STONE).model("mythic_sword"));
    }
}

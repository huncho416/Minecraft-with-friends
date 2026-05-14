package net.mythicpvp.suite.cosmetic;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticManagerTest {

    @Test
    void registersGrantsAndEquipsCosmetics() {
        CosmeticManager manager = CosmeticManager.getInstance();
        UUID player = UUID.randomUUID();
        manager.register(new CosmeticManager.Cosmetic("hat_crown", "Crown", CosmeticType.HAT, "Gold crown", NamespacedKey.fromString("mythic:hat_crown"), "COMMON", true, false));
        manager.grantCosmetic(player, "hat_crown");
        manager.equip(player, CosmeticType.HAT, "hat_crown");
        assertTrue(manager.ownsCosmetic(player, "HAT_CROWN"));
        assertEquals("hat_crown", manager.getEquipped(player, CosmeticType.HAT));
    }
}

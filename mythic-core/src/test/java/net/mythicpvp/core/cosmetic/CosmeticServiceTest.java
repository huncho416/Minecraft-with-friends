package net.mythicpvp.core.cosmetic;

import net.mythicpvp.core.persistence.CapturingPersistenceGateway;
import net.mythicpvp.suite.api.Currency;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CosmeticServiceTest {

    private CapturingPersistenceGateway gateway;
    private CosmeticService service;
    private UUID player;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        player = UUID.randomUUID();
        gateway = new CapturingPersistenceGateway();
        service = new CosmeticService(gateway, NamespacedKey.fromString("mythiccore:cosmetic_id"));

        CosmeticManager.getInstance().register(new CosmeticManager.Cosmetic(
                "hat_crown", "Crown", CosmeticType.HAT, "Gold crown",
                NamespacedKey.fromString("mythic:hat_crown"), "LEGENDARY", true, false));
        CosmeticManager.getInstance().register(new CosmeticManager.Cosmetic(
                "title_og", "OG", CosmeticType.TITLE, "Original player",
                null, "RARE", false, true));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void equipPersistsThroughGateway() {
        service.equip(player, CosmeticType.HAT, "hat_crown");

        assertEquals("hat_crown", CosmeticManager.getInstance().getEquipped(player, CosmeticType.HAT));
        assertEquals(1, gateway.calls.stream()
                .filter(c -> c instanceof CapturingPersistenceGateway.CosmeticEquip).count());
        CapturingPersistenceGateway.CosmeticEquip call =
                (CapturingPersistenceGateway.CosmeticEquip) gateway.calls.stream()
                        .filter(c -> c instanceof CapturingPersistenceGateway.CosmeticEquip).findFirst().orElseThrow();
        assertEquals(player, call.player());
        assertEquals("HAT", call.cosmeticType());
        assertEquals("hat_crown", call.cosmeticId());
    }

    @Test
    void unequipClearsSlot() {
        CosmeticManager.getInstance().equip(player, CosmeticType.HAT, "hat_crown");
        service.unequip(player, CosmeticType.HAT);

        assertNull(CosmeticManager.getInstance().getEquipped(player, CosmeticType.HAT));
    }

    @Test
    void withdrawCreatesItemWithPdc() {
        CosmeticManager.getInstance().grantCosmetic(player, "hat_crown");

        ItemStack item = service.withdraw(player, "hat_crown");

        assertNotNull(item);
        assertEquals(Material.PAPER, item.getType());
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        String pdcValue = meta.getPersistentDataContainer()
                .get(service.getCosmeticKey(), PersistentDataType.STRING);
        assertEquals("hat_crown", pdcValue);
    }

    @Test
    void withdrawReturnsNullForNonTradable() {
        CosmeticManager.getInstance().grantCosmetic(player, "title_og");

        ItemStack item = service.withdraw(player, "title_og");

        assertNull(item);
    }

    @Test
    void withdrawReturnsNullIfNotOwned() {
        ItemStack item = service.withdraw(player, "hat_crown");
        assertNull(item);
    }

    @Test
    void redeemGrantsOwnershipFromPdcItem() {
        CosmeticManager.getInstance().grantCosmetic(player, "hat_crown");
        ItemStack item = service.withdraw(player, "hat_crown");
        assertNotNull(item);

        UUID otherPlayer = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        boolean redeemed = service.redeem(otherPlayer, item);

        assertTrue(redeemed);
        assertTrue(CosmeticManager.getInstance().ownsCosmetic(otherPlayer, "hat_crown"));
    }

    @Test
    void redeemFailsIfAlreadyOwned() {
        CosmeticManager.getInstance().grantCosmetic(player, "hat_crown");
        ItemStack item = service.withdraw(player, "hat_crown");
        assertNotNull(item);

        boolean redeemed = service.redeem(player, item);
        assertFalse(redeemed);
    }

    @Test
    void purchaseDeductsBalanceAndGrants() {
        EconomyManager.getInstance().deposit(player, Currency.COINS, 500);

        boolean purchased = service.purchase(player, "hat_crown", Currency.COINS, 200);

        assertTrue(purchased);
        assertTrue(CosmeticManager.getInstance().ownsCosmetic(player, "hat_crown"));
        assertEquals(300, EconomyManager.getInstance().getBalance(player, Currency.COINS));
        assertTrue(gateway.calls.stream()
                .anyMatch(c -> c instanceof CapturingPersistenceGateway.CosmeticGrant));
    }

    @Test
    void purchaseFailsOnInsufficientBalance() {
        boolean purchased = service.purchase(player, "hat_crown", Currency.COINS, 200);

        assertFalse(purchased);
        assertFalse(CosmeticManager.getInstance().ownsCosmetic(player, "hat_crown"));
    }
}

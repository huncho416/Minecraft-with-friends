package net.mythicpvp.core.cosmetic;

import net.mythicpvp.core.persistence.CapturingPersistenceGateway;
import net.mythicpvp.suite.api.Currency;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.economy.EconomyManager;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CrateServiceTest {

    private CapturingPersistenceGateway gateway;
    private CrateService crateService;
    private UUID player;

    @BeforeEach
    void setUp() {
        player = UUID.randomUUID();
        gateway = new CapturingPersistenceGateway();
        CosmeticService cosmeticService = new CosmeticService(gateway, NamespacedKey.fromString("mythiccore:cosmetic_id"));
        crateService = new CrateService(cosmeticService, Logger.getLogger("test"));
        CosmeticManager.getInstance().register(new CosmeticManager.Cosmetic(
                "particle_hearts", "Hearts", CosmeticType.PARTICLE, "Love",
                NamespacedKey.fromString("mythic:particle_hearts"), "COMMON", true, false));
    }

    @Test
    void rollReturnsCosmeticFromPool() {
        EconomyManager.getInstance().deposit(player, Currency.COINS, 500);
        CrateDefinition crate = new CrateDefinition("test_crate", "Test Crate", 100, Currency.COINS,
                List.of(new CrateDefinition.CrateEntry("particle_hearts", 100)), 0, 0);

        CrateRoll roll = crateService.roll(player, crate);

        assertNotNull(roll);
        assertEquals("particle_hearts", roll.cosmeticId());
        assertEquals("test_crate", roll.crateId());
        assertTrue(CosmeticManager.getInstance().ownsCosmetic(player, "particle_hearts"));
    }

    @Test
    void rollDeductsCurrency() {
        EconomyManager.getInstance().deposit(player, Currency.COINS, 500);
        CrateDefinition crate = new CrateDefinition("test_crate", "Test Crate", 100, Currency.COINS,
                List.of(new CrateDefinition.CrateEntry("particle_hearts", 100)), 0, 0);

        crateService.roll(player, crate);

        assertEquals(400, EconomyManager.getInstance().getBalance(player, Currency.COINS));
    }

    @Test
    void rollFailsOnInsufficientBalance() {
        CrateDefinition crate = new CrateDefinition("test_crate", "Test Crate", 100, Currency.COINS,
                List.of(new CrateDefinition.CrateEntry("particle_hearts", 100)), 0, 0);

        CrateRoll roll = crateService.roll(player, crate);

        assertNull(roll);
    }

    @Test
    void singleEntryPoolIsDeterministic() {
        EconomyManager.getInstance().deposit(player, Currency.COINS, 1000);
        CrateDefinition crate = new CrateDefinition("test_crate", "Test Crate", 100, Currency.COINS,
                List.of(new CrateDefinition.CrateEntry("particle_hearts", 1)), 0, 0);

        CrateRoll roll = crateService.roll(player, crate);

        assertNotNull(roll);
        assertEquals("particle_hearts", roll.cosmeticId());
        assertEquals(100.0, roll.rollPercentage(), 0.01);
    }

    @Test
    void rollAppearsInAuditLog() {
        EconomyManager.getInstance().deposit(player, Currency.COINS, 500);
        CrateDefinition crate = new CrateDefinition("test_crate", "Test Crate", 100, Currency.COINS,
                List.of(new CrateDefinition.CrateEntry("particle_hearts", 100)), 0, 0);

        crateService.roll(player, crate);

        assertEquals(1, crateService.getAuditLog().size());
        assertEquals(player, crateService.getAuditLog().getFirst().player());
    }
}

package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.core.persistence.CoreHydrationSink;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.core.persistence.CapturingPersistenceGateway;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CosmeticHydrationTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);

    private CoreHydrationSink sink;

    @BeforeEach
    void setUp() {
        CosmeticManager.getInstance().register(new CosmeticManager.Cosmetic(
                "hat_crown", "Crown", CosmeticType.HAT, "Gold crown",
                NamespacedKey.fromString("mythic:hat_crown"), "LEGENDARY", true, false));

        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        RankService rankService = new RankService();
        rankService.setPersistence(gateway);
        GrantService grantService = new GrantService(rankService, FIXED_CLOCK);
        grantService.setPersistence(gateway);
        PunishmentService punishmentService = new PunishmentService(
                net.mythicpvp.suite.protocol.ProtocolManager.getInstance(), FIXED_CLOCK);
        punishmentService.setPersistence(gateway);
        SocialService socialService = new SocialService(gateway, FIXED_CLOCK);

        sink = new CoreHydrationSink(Logger.getLogger("test"),
                rankService, grantService, punishmentService, socialService);
    }

    @Test
    void applyCosmeticGrantPopulatesOwned() {
        sink.applyCosmeticGrant(PLAYER, "hat_crown", "HAT");

        assertTrue(CosmeticManager.getInstance().ownsCosmetic(PLAYER, "hat_crown"));
    }

    @Test
    void applyCosmeticEquipPopulatesEquipped() {
        CosmeticManager.getInstance().grantCosmetic(PLAYER, "hat_crown");

        sink.applyCosmeticEquip(PLAYER, "HAT", "hat_crown");

        assertEquals("hat_crown", CosmeticManager.getInstance().getEquipped(PLAYER, CosmeticType.HAT));
    }

    @Test
    void applyCosmeticEquipIgnoresUnknownType() {
        sink.applyCosmeticEquip(PLAYER, "INVALID_TYPE", "hat_crown");

        assertNull(CosmeticManager.getInstance().getEquipped(PLAYER, CosmeticType.HAT));
    }
}

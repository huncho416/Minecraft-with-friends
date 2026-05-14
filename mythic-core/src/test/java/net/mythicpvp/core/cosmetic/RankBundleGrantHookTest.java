package net.mythicpvp.core.cosmetic;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import net.mythicpvp.core.audit.CoreAuditLog;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the grant observer wiring: when {@code GrantService.grant}
 * succeeds, the registered {@link RankBundleGrantHook} fires once per
 * bundled cosmetic.
 *
 * <p>Asserts via {@link CosmeticManager#ownsCosmetic} since that's the
 * end state the hook is responsible for. The audit-log side-effect is
 * not asserted here — verified by inspection.
 */
class RankBundleGrantHookTest {

    private MockPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void grantingARankWithBundlesGrantsTheCosmetics() {
        // Set up: one VIP rank with two bundled cosmetics.
        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("vip", 100));
        GrantService grants = new GrantService(ranks, Clock.fixed(
                Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC));
        RankCosmeticBundles bundles = new RankCosmeticBundles();
        // No YAML — set bundle directly via a small reflection-free
        // path: load from a constructed config. Simplest is to just
        // bypass bundles.load and exercise the hook directly with a
        // bundle the hook can see.
        // For this test we'll wire the bundle by stuffing entries via
        // a config helper.
        // Actually — RankCosmeticBundles only loads from MythicConfig.
        // Build a tiny inline config.
        java.io.File data = plugin.getDataFolder();
        data.mkdirs();
        java.io.File file = new java.io.File(data, "ranks.yml");
        try {
            java.nio.file.Files.writeString(file.toPath(),
                    """
                    ranks:
                      vip:
                        bundled-cosmetics:
                          - "hat.party_crown"
                          - "title.vip"
                    """, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        bundles.load(new net.mythicpvp.suite.config.MythicConfig(plugin, "ranks.yml"));

        CoreAuditLog audit = new CoreAuditLog(plugin);
        grants.setGrantObserver(new RankBundleGrantHook(bundles, audit, plugin.getLogger()));

        UUID target = UUID.randomUUID();
        grants.grant(target, "Notch", "vip", GrantDuration.parse("permanent"),
                UUID.randomUUID(), "Console", "purchase");

        assertTrue(CosmeticManager.getInstance().ownsCosmetic(target, "hat.party_crown"));
        assertTrue(CosmeticManager.getInstance().ownsCosmetic(target, "title.vip"));
    }

    @Test
    void grantingARankWithNoBundlesIsANoOp() {
        // Sanity check: no bundle → no exception, no spurious grants.
        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("admin", 10));
        GrantService grants = new GrantService(ranks, Clock.fixed(
                Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC));
        RankCosmeticBundles bundles = new RankCosmeticBundles();
        // No bundle for admin loaded.
        CoreAuditLog audit = new CoreAuditLog(plugin);
        grants.setGrantObserver(new RankBundleGrantHook(bundles, audit, plugin.getLogger()));

        UUID target = UUID.randomUUID();
        grants.grant(target, "Steve", "admin", GrantDuration.parse("30d"),
                UUID.randomUUID(), "Console", "promotion");

        // Nothing to assert except that we got here without exception
        // and no spurious cosmetics are owned.
        assertTrue(CosmeticManager.getInstance().getOwned(target).isEmpty());
    }

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE,
                "&7", "", weight, false, false, "",
                List.of(),
                "&7", "%chat_prefix%%player%: %message%",
                "&7", "%tab_prefix%%player%",
                "&7", "%nametag_prefix%%player%");
    }
}

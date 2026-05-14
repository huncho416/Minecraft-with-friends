package net.mythicpvp.core.cosmetic;

import net.mythicpvp.core.audit.CoreAuditLog;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.config.MythicConfig;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankBundleGrantHookTest {

    @TempDir
    Path tempDir;

    @Test
    void grantingARankWithBundlesGrantsTheCosmetics() {

        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("vip", 100));
        GrantService grants = new GrantService(ranks, Clock.fixed(
                Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC));
        RankCosmeticBundles bundles = new RankCosmeticBundles();

        File data = tempDir.toFile();
        data.mkdirs();
        File file = new File(data, "ranks.yml");
        try {
            Files.writeString(file.toPath(),
                    """
                    ranks:
                      vip:
                        bundled-cosmetics:
                          - "hat.party_crown"
                          - "title.vip"
                    """, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bundles.load(new MythicConfig(data, "ranks.yml"));

        Plugin plugin = plugin();
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

        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("admin", 10));
        GrantService grants = new GrantService(ranks, Clock.fixed(
                Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC));
        RankCosmeticBundles bundles = new RankCosmeticBundles();

        Plugin plugin = plugin();
        CoreAuditLog audit = new CoreAuditLog(plugin);
        grants.setGrantObserver(new RankBundleGrantHook(bundles, audit, plugin.getLogger()));

        UUID target = UUID.randomUUID();
        grants.grant(target, "Steve", "admin", GrantDuration.parse("30d"),
                UUID.randomUUID(), "Console", "promotion");

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

    private Plugin plugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("RankBundleGrantHookTest"));
        return plugin;
    }
}

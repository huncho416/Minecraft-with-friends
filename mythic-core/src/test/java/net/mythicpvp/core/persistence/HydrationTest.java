package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydrationTest {

    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STAFF = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void coreSinkRoutesRanksThroughApplyRank() {
        RankService ranks = new RankService();
        GrantService grants = new GrantService(ranks, FIXED_CLOCK);
        PunishmentService punishments = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        CoreHydrationSink sink = new CoreHydrationSink(Logger.getLogger("test"), ranks, grants, punishments);

        CoreRank vip = rank("vip", 100);
        sink.applyRank(vip);
        assertEquals(vip, ranks.get("vip"));

        sink.removeRank("vip");
        assertNull(ranks.get("vip"));
    }

    @Test
    void coreSinkRoutesGrantsThroughApplyGrantPreservingId() {
        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("admin", 10));
        GrantService grants = new GrantService(ranks, FIXED_CLOCK);
        PunishmentService punishments = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        CoreHydrationSink sink = new CoreHydrationSink(Logger.getLogger("test"), ranks, grants, punishments);

        RankGrant fromStdb = new RankGrant(
                42L, PLAYER, "Notch", "admin", STAFF, "Console", "from another server",
                FIXED_CLOCK.millis(), 0L, true);
        sink.applyGrant(fromStdb);

        assertEquals(1, grants.history(PLAYER).size());
        assertEquals(42L, grants.history(PLAYER).get(0).id());
        assertEquals("admin", grants.activeRank(PLAYER));

        RankGrant local = grants.grant(
                UUID.randomUUID(), "Other", "admin",
                net.mythicpvp.core.rank.GrantDuration.parse("permanent"),
                STAFF, "Console", "test");
        assertTrue(local.id() > 42L,
                "local grants after hydration must skip ahead of STDB-assigned ids; got " + local.id());

        sink.removeGrant(42L);
        assertEquals(0, grants.history(PLAYER).size());
    }

    @Test
    void coreSinkRoutesPunishmentsThroughApplyRecordPreservingId() {
        RankService ranks = new RankService();
        GrantService grants = new GrantService(ranks, FIXED_CLOCK);
        PunishmentService punishments = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        CoreHydrationSink sink = new CoreHydrationSink(Logger.getLogger("test"), ranks, grants, punishments);

        PunishmentRecord fromStdb = new PunishmentRecord(
                99L, PLAYER, "Notch", STAFF, "Admin",
                PunishmentType.TEMP_BAN, "exploit", "screenshot.png",
                FIXED_CLOCK.millis(),
                FIXED_CLOCK.millis() + 3_600_000L,
                false, true, false, "hub-1");
        sink.applyPunishment(fromStdb);

        assertEquals(1, punishments.history(PLAYER).size());
        assertEquals(99L, punishments.history(PLAYER).get(0).id());

        sink.removePunishment(99L);
        assertEquals(0, punishments.history(PLAYER).size());
    }

    @Test
    void coreSinkRoutesTemplatesThroughApplyTemplateRow() {
        RankService ranks = new RankService();
        GrantService grants = new GrantService(ranks, FIXED_CLOCK);
        PunishmentService punishments = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        CoreHydrationSink sink = new CoreHydrationSink(Logger.getLogger("test"), ranks, grants, punishments);

        PunishmentTemplate cheating = new PunishmentTemplate(
                PunishmentCategory.BAN, "30d", "Cheating #1", "First offense.");
        sink.applyTemplate(cheating);

        assertNotNull(punishments.template("Cheating #1"));
        assertEquals("30d", punishments.template("Cheating #1").duration());

        PunishmentTemplate updated = new PunishmentTemplate(
                PunishmentCategory.BAN, "60d", "Cheating #1", "Updated description.");
        sink.applyTemplate(updated);
        assertEquals("60d", punishments.template("Cheating #1").duration());
        assertEquals("Updated description.", punishments.template("Cheating #1").information());
        assertEquals(1, punishments.templates().size(), "title is the natural key — no duplicate row");

        sink.removeTemplate("Cheating #1");
        assertNull(punishments.template("Cheating #1"));
    }

    @Test
    void coreSinkTracksBlacklistActiveFlag() {
        RankService ranks = new RankService();
        GrantService grants = new GrantService(ranks, FIXED_CLOCK);
        PunishmentService punishments = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        CoreHydrationSink sink = new CoreHydrationSink(Logger.getLogger("test"), ranks, grants, punishments);

        sink.applyBlacklist(PLAYER, "Notch", true);
        assertTrue(sink.isBlacklisted(PLAYER));
        assertEquals(1, sink.blacklistedUuids().size());

        sink.applyBlacklist(PLAYER, "Notch", false);
        assertFalse(sink.isBlacklisted(PLAYER));
        assertEquals(0, sink.blacklistedUuids().size());
    }

    @Test
    void hydratedRanksDoNotEchoBackToGateway() {

        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        RankService ranks = new RankService();
        ranks.setPersistence(gateway);

        ranks.applyRank(rank("vip", 100));
        ranks.removeRank("vip");

        assertEquals(0, gateway.calls.size(),
                "apply* / remove* must bypass the gateway; only register() / setField() echo out");
    }

    @Test
    void hydratedGrantsDoNotEchoBackToGateway() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("admin", 10));
        GrantService grants = new GrantService(ranks, FIXED_CLOCK);
        grants.setPersistence(gateway);

        gateway.calls.clear();

        grants.applyGrant(new RankGrant(7L, PLAYER, "Notch", "admin",
                STAFF, "Console", "", FIXED_CLOCK.millis(), 0L, true));
        grants.removeGrant(7L);

        assertEquals(0, gateway.calls.size(),
                "applyGrant / removeGrant must bypass the gateway");
    }

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE, "&7", "", weight, false, false, "",
                List.of(), "&7", "%chat_prefix%%player%: %message%",
                "&7", "%tab_prefix%%player%",
                "&7", "%nametag_prefix%%player%");
    }
}

package net.mythicpvp.core.rank;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GrantServiceTest {

    @Test
    void activeRankUsesLowestWeightActiveGrant() {
        RankService rankService = new RankService();
        rankService.register(rank("default", 1000));
        rankService.register(rank("admin", 10));
        rankService.register(rank("owner", 1));
        GrantService grantService = new GrantService(rankService, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();

        grantService.grant(target, "Target", "admin", GrantDuration.parse("30d"), UUID.randomUUID(), "Console", "Staff");
        grantService.grant(target, "Target", "owner", GrantDuration.parse("1d"), UUID.randomUUID(), "Console", "Owner");

        assertEquals("owner", grantService.activeRank(target));
        assertEquals(2, grantService.history(target).size());
    }

    @Test
    void inactiveGrantRemainsInHistoryUntilRemoved() {
        RankService rankService = new RankService();
        rankService.register(rank("default", 1000));
        rankService.register(rank("admin", 10));
        GrantService grantService = new GrantService(rankService, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();

        RankGrant grant = grantService.grant(target, "Target", "admin", GrantDuration.parse("permanent"), UUID.randomUUID(), "Console", "Staff");

        assertTrue(grantService.deactivate(grant.id()));
        assertEquals(1, grantService.history(target).size());
        assertEquals("default", grantService.activeRank(target));
        assertTrue(grantService.removeInactive(grant.id()));
        assertTrue(grantService.history(target).isEmpty());
    }

    @Test
    void displayKeepsChatTabAndNametagFormatsSeparate() {
        RankService rankService = new RankService();
        rankService.register(new CoreRank("owner", "Owner", "#DE2222", Material.RED_DYE, "&c", "", 1, true, false, "", List.of("*"), "chat", "chat-format", "tab", "tab-format", "name", "name-format"));

        RankDisplay display = rankService.display("owner");

        assertEquals("chat", display.chatPrefix());
        assertEquals("tab", display.tabPrefix());
        assertEquals("name", display.nametagPrefix());
    }

    @Test
    void rankEditorFieldsMutateRuntimeRank() {
        RankService rankService = new RankService();
        rankService.register(rank("admin", 10));

        assertTrue(rankService.setField("admin", "chat-prefix", "&#FF00F8ADMIN"));
        assertTrue(rankService.setField("admin", "staff", "true"));
        assertTrue(rankService.setField("admin", "dye", "RED_DYE"));
        assertTrue(rankService.addPermission("admin", "mythic.core.test"));
        assertTrue(rankService.removePermission("admin", "mythic.core.test"));

        CoreRank rank = rankService.get("admin");
        assertNotNull(rank);
        assertEquals("&#FF00F8ADMIN", rank.chatPrefix());
        assertTrue(rank.staff());
        assertEquals(Material.RED_DYE, rank.dye());
        assertFalse(rank.permissions().contains("mythic.core.test"));
    }

    @Test
    void invalidRankEditorWeightDoesNotMutateRank() {
        RankService rankService = new RankService();
        rankService.register(rank("admin", 10));

        assertFalse(rankService.setField("admin", "weight", "highest"));
        assertEquals(10, rankService.get("admin").weight());
    }

    @Test
    void clearRemovesAllGrantsForTarget() {
        RankService rankService = new RankService();
        rankService.register(rank("default", 1000));
        rankService.register(rank("admin", 10));
        rankService.register(rank("mod", 50));
        GrantService grantService = new GrantService(rankService, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        grantService.grant(target, "Target", "admin", GrantDuration.parse("permanent"), UUID.randomUUID(), "Console", "Staff");
        grantService.grant(target, "Target", "mod", GrantDuration.parse("7d"), UUID.randomUUID(), "Console", "Upgrade");
        grantService.grant(other, "Other", "admin", GrantDuration.parse("permanent"), UUID.randomUUID(), "Console", "Staff");

        assertEquals(2, grantService.clear(target));
        assertTrue(grantService.history(target).isEmpty());
        assertEquals("default", grantService.activeRank(target));
        assertEquals(1, grantService.history(other).size());
    }

    @Test
    void cgrantStyleGrantPersistsWithDurationAndReason() {
        RankService rankService = new RankService();
        rankService.register(rank("default", 1000));
        rankService.register(rank("vip", 100));
        GrantService grantService = new GrantService(rankService, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();
        UUID executor = UUID.randomUUID();

        RankGrant grant = grantService.grant(target, "TargetPlayer", "vip", GrantDuration.parse("30d"), executor, "Admin", "Purchased");

        assertEquals("vip", grant.rankId());
        assertEquals(target, grant.targetUuid());
        assertEquals(executor, grant.executorUuid());
        assertEquals("Purchased", grant.reason());
        assertTrue(grant.expiresAtMillis() > 0);
        assertFalse(grant.permanent());
        assertEquals("vip", grantService.activeRank(target));
    }

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE, "&7", "", weight, false, false, "", List.of(), "&7", "%chat_prefix%%player%: %message%", "&7", "%tab_prefix%%player%", "&7", "%nametag_prefix%%player%");
    }
}

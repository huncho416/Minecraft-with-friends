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

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE, "&7", "", weight, false, false, "", List.of(), "&7", "%chat_prefix%%player%: %message%", "&7", "%tab_prefix%%player%", "&7", "%nametag_prefix%%player%");
    }
}

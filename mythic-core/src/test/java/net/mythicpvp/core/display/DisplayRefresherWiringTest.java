package net.mythicpvp.core.display;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.rank.RankService;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayRefresherWiringTest {

    private static final UUID TARGET = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STAFF = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Clock FIXED = Clock.fixed(
            Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void rankRegisterAfterSeedingCallsRefresher() {
        AtomicInteger calls = new AtomicInteger();
        RankService ranks = new RankService();
        ranks.setDisplayRefresher(calls::incrementAndGet);

        ranks.register(rank("vip", 100));
        assertEquals(1, calls.get());

        assertTrue(ranks.setField("vip", "weight", "50"));
        assertEquals(2, calls.get());
    }

    @Test
    void rankApplyRankFromHydrationCallsRefresher() {

        AtomicInteger calls = new AtomicInteger();
        RankService ranks = new RankService();
        ranks.setDisplayRefresher(calls::incrementAndGet);

        ranks.applyRank(rank("vip", 100));
        ranks.removeRank("vip");
        assertEquals(2, calls.get());
    }

    @Test
    void grantOperationsCallRefresherWithTargetUuid() {
        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("admin", 10));
        GrantService grants = new GrantService(ranks, FIXED);

        List<UUID> refreshed = new ArrayList<>();
        grants.setDisplayRefresher(refreshed::add);

        RankGrant g = grants.grant(TARGET, "Notch", "admin",
                GrantDuration.parse("permanent"), STAFF, "Console", "test");

        assertEquals(1, refreshed.size());
        assertEquals(TARGET, refreshed.get(0));

        grants.deactivate(g.id());
        grants.clear(TARGET);

        assertEquals(3, refreshed.size());
        assertTrue(refreshed.stream().allMatch(uuid -> uuid.equals(TARGET)));
    }

    @Test
    void grantApplyGrantFromHydrationCallsRefresher() {
        RankService ranks = new RankService();
        ranks.register(rank("default", 1000));
        ranks.register(rank("admin", 10));
        GrantService grants = new GrantService(ranks, FIXED);

        AtomicInteger calls = new AtomicInteger();
        grants.setDisplayRefresher(uuid -> calls.incrementAndGet());

        grants.applyGrant(new RankGrant(7L, TARGET, "Notch", "admin",
                STAFF, "Console", "", FIXED.millis(), 0L, true));
        grants.removeGrant(7L);

        assertEquals(2, calls.get(),
                "hydration apply/remove both refresh display");
    }

    @Test
    void seedingDoesNotFireRefresher() {

        AtomicInteger calls = new AtomicInteger();
        RankService ranks = new RankService();
        ranks.setDisplayRefresher(calls::incrementAndGet);

        RankService bareRanks = new RankService();
        bareRanks.register(rank("default", 1000));

        assertEquals(0, calls.get(), "default no-op refresher must be silent");
    }

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE,
                "&7", "", weight, false, false, "",
                List.of(),
                "&7", "%chat_prefix%%player%: %message%",
                "&7", "%tab_prefix%%player%",
                "&7", "%nametag_prefix%%player%",
                CoreRank.SCOPE_GLOBAL);
    }
}

package net.mythicpvp.core.chat;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the Phase 3 additions to {@link ChatControlService}:
 *
 * <ul>
 *   <li>scope-based filtering — LOCAL state from another shard is dropped
 *   <li>{@code clear()} fires registered {@link ChatControlService.ClearListener}
 *       callbacks exactly once per pulse
 *   <li>{@code registerMessage} enforces the slow-mode cool-down
 * </ul>
 *
 * <p>Friend's original {@code ChatControlServiceTest} still passes —
 * those tests exercise the legacy single-arg constructor (empty
 * {@code shardId}, match-all) which we preserved on purpose.
 */
class ChatControlServiceExtensionsTest {

    private final ProtocolManager protocolManager = ProtocolManager.getInstance();

    @AfterEach
    void tearDown() {
        protocolManager.clear();
    }

    @Test
    void localScopeFromAnotherShardIsDropped() {
        ChatControlService hub = new ChatControlService(protocolManager, "hub-1");
        ChatControlService skyblock = new ChatControlService(protocolManager, "sb-1");

        // Hub mutes locally. Skyblock should NOT see this — LOCAL means
        // "originating shard only". Hub itself does see and apply.
        hub.mute(ChatScope.LOCAL);

        assertTrue(hub.muted(), "originating shard applies its own LOCAL change");
        assertFalse(skyblock.muted(), "foreign shard ignores LOCAL state from elsewhere");
    }

    @Test
    void networkScopeReplicatesAcrossShards() {
        ChatControlService hub = new ChatControlService(protocolManager, "hub-1");
        ChatControlService skyblock = new ChatControlService(protocolManager, "sb-1");

        hub.mute(ChatScope.NETWORK);

        assertTrue(hub.muted());
        assertTrue(skyblock.muted(), "NETWORK scope reaches every shard");
    }

    @Test
    void clearPulseFiresListenerExactlyOnce() {
        ChatControlService service = new ChatControlService(protocolManager, "hub-1");
        AtomicInteger fires = new AtomicInteger();
        service.onClear(fires::incrementAndGet);

        service.clear(ChatScope.LOCAL);
        assertEquals(1, fires.get(), "one clear() call → one listener fire");

        service.clear(ChatScope.LOCAL);
        assertEquals(2, fires.get(), "subsequent clear() calls also fire");
    }

    @Test
    void mutateWithoutClearDoesNotFireClearListener() {
        ChatControlService service = new ChatControlService(protocolManager, "hub-1");
        AtomicInteger fires = new AtomicInteger();
        service.onClear(fires::incrementAndGet);

        service.mute(ChatScope.LOCAL);
        service.slow(5, ChatScope.LOCAL);
        service.unmute(ChatScope.LOCAL);

        assertEquals(0, fires.get(),
                "mute/slow/unmute don't bump clearTick — listener stays quiet");
    }

    @Test
    void clearReplicatesAcrossShardsViaNetworkScope() {
        ChatControlService hub = new ChatControlService(protocolManager, "hub-1");
        ChatControlService skyblock = new ChatControlService(protocolManager, "sb-1");
        AtomicInteger hubFires = new AtomicInteger();
        AtomicInteger sbFires = new AtomicInteger();
        hub.onClear(hubFires::incrementAndGet);
        skyblock.onClear(sbFires::incrementAndGet);

        hub.clear(ChatScope.NETWORK);

        assertEquals(1, hubFires.get());
        assertEquals(1, sbFires.get(), "NETWORK clear pulse hits every shard");
    }

    @Test
    void registerMessageReturnsZeroWhenSlowDisabled() {
        ChatControlService service = new ChatControlService(protocolManager, "hub-1");
        UUID player = UUID.randomUUID();
        long now = 1_000_000L;

        // No slow mode set — should always allow.
        assertEquals(0L, service.registerMessage(player, now));
        assertEquals(0L, service.registerMessage(player, now + 50));
        assertEquals(0L, service.registerMessage(player, now + 51));
    }

    @Test
    void registerMessageEnforcesCooldownWhenSlow() {
        ChatControlService service = new ChatControlService(protocolManager, "hub-1");
        service.slow(5, ChatScope.LOCAL);
        UUID player = UUID.randomUUID();
        long now = 1_000_000L;

        // First message always allowed, sets the cool-down anchor.
        assertEquals(0L, service.registerMessage(player, now));

        // 1 second later — must wait 4 more seconds.
        long wait = service.registerMessage(player, now + 1_000L);
        assertEquals(4_000L, wait);

        // Right at the cool-down boundary, still 1 ms short.
        wait = service.registerMessage(player, now + 4_999L);
        assertEquals(1L, wait);

        // Past the boundary, allowed and resets the anchor.
        assertEquals(0L, service.registerMessage(player, now + 5_001L));

        // The fresh anchor means the next attempt must wait 5s again.
        long wait2 = service.registerMessage(player, now + 5_002L);
        assertTrue(wait2 > 0, "anchor reset means the next message starts a new cool-down");
    }

    @Test
    void forgetDropsPlayerSlowState() {
        ChatControlService service = new ChatControlService(protocolManager, "hub-1");
        service.slow(5, ChatScope.LOCAL);
        UUID player = UUID.randomUUID();
        long now = 1_000_000L;

        service.registerMessage(player, now);
        assertTrue(service.registerMessage(player, now + 100L) > 0,
                "still in cool-down before forget");

        service.forget(player);

        // After forget, the player has no anchor — first message free again.
        assertEquals(0L, service.registerMessage(player, now + 200L));
    }

    @Test
    void slowSecondsAndMutedHelpersMatchState() {
        ChatControlService service = new ChatControlService(protocolManager, "hub-1");
        service.mute(ChatScope.LOCAL);
        service.slow(7, ChatScope.LOCAL);

        assertTrue(service.muted());
        assertEquals(7, service.slowSeconds());
        assertEquals(service.state().muted(), service.muted());
        assertEquals(service.state().slowSeconds(), service.slowSeconds());
    }
}

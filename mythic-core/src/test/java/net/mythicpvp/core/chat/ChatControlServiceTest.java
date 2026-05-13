package net.mythicpvp.core.chat;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatControlServiceTest {

    private final ProtocolManager protocolManager = ProtocolManager.getInstance();

    @AfterEach
    void tearDown() {
        protocolManager.clear();
    }

    @Test
    void networkSlowModeReplicatesThroughProtocol() {
        ChatControlService skyblock = new ChatControlService(protocolManager);
        ChatControlService hub = new ChatControlService(protocolManager);
        skyblock.slow(5, ChatScope.NETWORK);
        assertEquals(5, hub.state().slowSeconds());
        assertEquals(ChatScope.NETWORK, hub.state().scope());
        assertEquals(1, hub.history().size());
    }

    @Test
    void negativeSlowModeIsRejected() {
        ChatControlService service = new ChatControlService(protocolManager);
        assertThrows(IllegalArgumentException.class, () -> service.slow(-1, ChatScope.LOCAL));
    }
}

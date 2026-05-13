package net.mythicpvp.core.staff;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StaffChannelServiceTest {

    private final ProtocolManager protocolManager = ProtocolManager.getInstance();

    @AfterEach
    void tearDown() {
        protocolManager.clear();
    }

    @Test
    void sendsCrossServerStaffMessageWithServerAndRankColor() {
        StaffChannelService skyblock = new StaffChannelService(protocolManager, "skyblock");
        StaffChannelService hub = new StaffChannelService(protocolManager, "hub");
        List<StaffMessage> received = new ArrayList<>();
        hub.addAudience(received::add);
        skyblock.send(StaffChannel.STAFF, UUID.randomUUID(), "AdminName", "Admin", "&#FF5555", "Hello staff");
        assertEquals(1, received.size());
        StaffMessage message = received.getFirst();
        assertEquals("skyblock", message.server());
        assertEquals("&#FF5555", message.rankColor());
        assertEquals("Hello staff", message.message());
        assertEquals(1, skyblock.history().size());
        assertEquals(1, hub.history().size());
    }
}

package net.mythicpvp.suite.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolManagerTest {

    private final ProtocolManager manager = ProtocolManager.getInstance();

    @AfterEach
    void cleanup() {
        manager.clear();
    }

    @Test
    void publishesSerializedMessagesToSubscribers() {
        AtomicReference<ProtocolManager.ProtocolMessage> seen = new AtomicReference<>();
        manager.subscribe("hub", seen::set);
        manager.publish("hub", Map.of("server", "hub-1"));
        assertEquals("hub", seen.get().channel());
        assertEquals("hub-1", seen.get().deserialize(ServerPayload.class).server());
    }

    record ServerPayload(String server) {}
}

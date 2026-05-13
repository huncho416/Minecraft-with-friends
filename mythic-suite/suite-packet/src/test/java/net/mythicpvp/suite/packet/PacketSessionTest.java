package net.mythicpvp.suite.packet;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PacketSessionTest {

    private final PacketSession session = PacketSession.getInstance();

    @AfterEach
    void cleanup() {
        session.clear();
    }

    @Test
    void recordsAndRendersActionsPerViewer() {
        UUID viewerId = UUID.randomUUID();
        Player viewer = mock(Player.class);
        when(viewer.getUniqueId()).thenReturn(viewerId);
        AtomicInteger renders = new AtomicInteger();
        session.setRenderer((player, action) -> renders.incrementAndGet());
        session.send(viewer, new PacketAction.TabHeaderFooter("tab", Component.text("h"), Component.text("f"), Map.of()));
        assertEquals(1, renders.get());
        assertEquals(1, session.getActions(viewerId).size());
        assertEquals(1, session.drain(viewerId).size());
        assertEquals(0, session.getActions(viewerId).size());
    }
}

package net.mythicpvp.suite.nametag;

import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.packet.PacketAction;
import net.mythicpvp.suite.packet.PacketSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NametagManagerTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void cleanup() {
        NametagManager.getInstance().clear();
        DisguiseManager.getInstance().clear();
        PacketSession.getInstance().clear();
        MockBukkit.unmock();
    }

    @Test
    void emitsNametagStateWithGlowAndDisguise() {
        PlayerMock viewer = server.addPlayer("Viewer");
        PlayerMock target = server.addPlayer("Target");
        DisguiseManager.getInstance().disguiseAs(target.getUniqueId(), "Nick", null, null);
        NametagManager.getInstance().setNametag(target, "&a", "&f", 5, "green");
        assertEquals("green", NametagManager.getInstance().getNametag(target).glowColor());
        assertTrue(PacketSession.getInstance().getActions(viewer.getUniqueId()).stream()
            .anyMatch(PacketAction.NametagState.class::isInstance));
    }
}

package net.mythicpvp.suite.tab;

import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.packet.PacketSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabManagerTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void cleanup() {
        TabManager.getInstance().clear();
        DisguiseManager.getInstance().clear();
        PacketSession.getInstance().clear();
        MockBukkit.unmock();
    }

    @Test
    void emitsTabPacketWithDisguiseAwareEntries() {
        PlayerMock viewer = server.addPlayer("Viewer");
        PlayerMock target = server.addPlayer("Target");
        DisguiseManager.getInstance().disguiseAs(target.getUniqueId(), "Nick", null, null);
        TabManager.getInstance().setDefaults("Header", "Footer");
        TabManager.getInstance().apply(viewer);
        assertEquals(2, PacketSession.getInstance().getActions(viewer.getUniqueId()).size());
        assertEquals(2, TabManager.getInstance().visibleEntries(viewer).size());
    }
}

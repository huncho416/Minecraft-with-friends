package net.mythicpvp.suite.hologram;

import net.mythicpvp.suite.packet.PacketSession;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramManagerTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void cleanup() {
        HologramManager.getInstance().removeAll();
        PacketSession.getInstance().clear();
        MockBukkit.unmock();
    }

    @Test
    void emitsPerPlayerLeaderboardHologramPackets() {
        PlayerMock player = server.addPlayer("Player");
        HologramManager.Hologram hologram = HologramManager.getInstance().create("top", new Location(null, 0, 0, 0), List.of("Top"), true, true);
        hologram.setAnimationFrames(List.of("a", "b"));
        hologram.showTo(player);
        hologram.tickAnimation();
        assertTrue(hologram.isLeaderboard());
        assertEquals(2, PacketSession.getInstance().getActions(player.getUniqueId()).size());
    }
}

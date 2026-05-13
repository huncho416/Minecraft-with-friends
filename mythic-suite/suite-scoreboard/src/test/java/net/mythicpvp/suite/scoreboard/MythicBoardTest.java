package net.mythicpvp.suite.scoreboard;

import net.mythicpvp.suite.packet.PacketSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MythicBoardTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void cleanup() {
        PacketSession.getInstance().clear();
        MockBukkit.unmock();
    }

    @Test
    void emitsScoreboardStateAndAnimatesTitle() {
        PlayerMock player = server.addPlayer("Player");
        MythicBoard board = new MythicBoard(player, "One");
        board.setAnimatedTitles(List.of("One", "Two"));
        board.tickTitleAnimation();
        board.setLines(List.of("A", "B"));
        assertEquals(List.of("A", "B"), board.getLines());
        assertEquals(4, PacketSession.getInstance().getActions(player.getUniqueId()).size());
    }
}

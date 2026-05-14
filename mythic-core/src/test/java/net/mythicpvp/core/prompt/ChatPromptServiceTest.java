package net.mythicpvp.core.prompt;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ChatPromptServiceTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void consumesNextChatMessageForPrompt() {
        ChatPromptService service = new ChatPromptService(mock(Plugin.class));
        PlayerMock player = server.addPlayer("Executor");
        AtomicReference<String> value = new AtomicReference<>();

        service.await(player, (p, input) -> value.set(input));
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, player, "7d", java.util.Set.of());
        service.onChat(event);

        assertTrue(event.isCancelled());
        assertEquals("7d", value.get());
        assertFalse(service.waiting(player.getUniqueId()));
    }

    @Test
    void cancelSkipsPromptHandler() {
        ChatPromptService service = new ChatPromptService(mock(Plugin.class));
        PlayerMock player = server.addPlayer("Executor");
        AtomicReference<String> value = new AtomicReference<>();

        service.await(player, (p, input) -> value.set(input));
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, player, "cancel", java.util.Set.of());
        service.onChat(event);

        assertTrue(event.isCancelled());
        assertNull(value.get());
    }
}

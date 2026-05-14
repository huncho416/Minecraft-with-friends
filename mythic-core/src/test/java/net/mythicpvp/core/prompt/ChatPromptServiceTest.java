package net.mythicpvp.core.prompt;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        ChatPromptService service = new ChatPromptService(null);
        PlayerMock player = server.addPlayer("Executor");
        AtomicReference<String> value = new AtomicReference<>();

        service.await(player, (p, input) -> value.set(input));

        AtomicBoolean cancelled = new AtomicBoolean();
        service.handleChat(player, Component.text("7d"), false, () -> cancelled.set(true));

        assertTrue(cancelled.get());
        assertEquals("7d", value.get());
        assertFalse(service.waiting(player.getUniqueId()));
    }

    @Test
    void cancelSkipsPromptHandler() {
        ChatPromptService service = new ChatPromptService(null);
        PlayerMock player = server.addPlayer("Executor");
        AtomicReference<String> value = new AtomicReference<>();

        service.await(player, (p, input) -> value.set(input));
        AtomicBoolean cancelled = new AtomicBoolean();
        service.handleChat(player, Component.text("cancel"), false, () -> cancelled.set(true));

        assertTrue(cancelled.get());
        assertNull(value.get());
    }
}

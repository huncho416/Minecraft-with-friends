package net.mythicpvp.core.prompt;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        ChatPromptService service = new ChatPromptService(mock(JavaPlugin.class));
        PlayerMock player = server.addPlayer("Executor");
        AtomicReference<String> value = new AtomicReference<>();

        service.await(player, (p, input) -> value.set(input));

        AsyncChatEvent event = mockChat(player, "7d");
        service.onChat(event);

        assertTrue(cancelledFlagOf(event).get());
        assertEquals("7d", value.get());
        assertFalse(service.waiting(player.getUniqueId()));
    }

    @Test
    void cancelSkipsPromptHandler() {
        ChatPromptService service = new ChatPromptService(mock(JavaPlugin.class));
        PlayerMock player = server.addPlayer("Executor");
        AtomicReference<String> value = new AtomicReference<>();

        service.await(player, (p, input) -> value.set(input));
        AsyncChatEvent event = mockChat(player, "cancel");
        service.onChat(event);

        assertTrue(cancelledFlagOf(event).get());
        assertNull(value.get());
    }

    private AsyncChatEvent mockChat(PlayerMock player, String text) {
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(Component.text(text));
        when(event.isAsynchronous()).thenReturn(false);

        AtomicBoolean cancelled = new AtomicBoolean();
        doAnswer(invocation -> {
            cancelled.set(invocation.getArgument(0));
            return null;
        }).when(event).setCancelled(anyBoolean());
        when(event.isCancelled()).thenAnswer(inv -> cancelled.get());

        cancelledFlags.put(System.identityHashCode(event), cancelled);
        return event;
    }

    private final java.util.Map<Integer, AtomicBoolean> cancelledFlags = new java.util.HashMap<>();

    private AtomicBoolean cancelledFlagOf(AsyncChatEvent event) {
        return cancelledFlags.get(System.identityHashCode(event));
    }
}

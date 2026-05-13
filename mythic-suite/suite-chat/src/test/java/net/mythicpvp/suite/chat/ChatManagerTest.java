package net.mythicpvp.suite.chat;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.suite.disguise.DisguiseManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatManagerTest {

    @AfterEach
    void cleanup() {
        DisguiseManager.getInstance().clear();
    }

    @Test
    void filtersAdsAndUsesDisguiseName() {
        ChatManager manager = ChatManager.getInstance();
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Real");
        DisguiseManager.getInstance().disguiseAs(uuid, "Nick", null, null);
        assertTrue(manager.isBlocked("join example.com"));
        assertEquals(" Nick: visit ***", PlainTextComponentSerializer.plainText().serialize(manager.format(player, "", "visit example.com")));
    }
}

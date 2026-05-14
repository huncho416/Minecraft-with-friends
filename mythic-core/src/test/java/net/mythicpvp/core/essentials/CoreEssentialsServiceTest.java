package net.mythicpvp.core.essentials;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import net.kyori.adventure.text.Component;
import net.mythicpvp.core.command.CoreCompletionValues;
import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreEssentialsServiceTest {

    private ServerMock server;
    private CoreEssentialsService service;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        service = new CoreEssentialsService(new TestMessages());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void gamemodeShortcutsSetSenderGamemode() {
        PlayerMock player = server.addPlayer("Admin");

        service.setGameMode(player, "creative", null);
        assertEquals(GameMode.CREATIVE, player.getGameMode());
        service.setGameMode(player, "survival", null);
        assertEquals(GameMode.SURVIVAL, player.getGameMode());
    }

    @Test
    void gamemodeTargetRequiresOthersPermissionAndCompletesOnlyWhenAllowed() {
        PlayerMock admin = server.addPlayer("Admin");
        PlayerMock target = server.addPlayer("Target");

        assertEquals(List.of("creative", "survival", "adventure", "spectator"), CoreCompletionValues.gamemodes());
        assertTrue(CoreCompletionValues.gamemodeTargets(admin).isEmpty());
        service.setGameMode(admin, "creative", "Target");
        assertEquals(GameMode.SURVIVAL, target.getGameMode());

        admin.setOp(true);

        assertTrue(CoreCompletionValues.gamemodeTargets(admin).contains("Target"));
        service.setGameMode(admin, "creative", "Target");
        assertEquals(GameMode.CREATIVE, target.getGameMode());
    }

    @Test
    void teleportCommandsMovePlayers() {
        World world = server.addSimpleWorld("world");
        PlayerMock admin = server.addPlayer("Admin");
        PlayerMock target = server.addPlayer("Target");
        PlayerMock destination = server.addPlayer("Destination");
        admin.teleport(new Location(world, 10, 65, 10));
        target.teleport(new Location(world, 20, 65, 20));
        destination.teleport(new Location(world, 40, 70, 40));
        admin.setOp(true);

        service.teleport(admin, "Target", "Destination");
        assertEquals(destination.getLocation().getBlockX(), target.getLocation().getBlockX());
        service.teleportHere(admin, "Target");
        assertEquals(admin.getLocation().getBlockX(), target.getLocation().getBlockX());
        assertTrue(CoreCompletionValues.teleportOthers(admin).contains("Destination"));
    }

    @Test
    void publicHelpAndDiscordCommandsDispatch() {
        PlayerMock player = server.addPlayer("Player");

        service.sendHelp(player);
        service.sendDiscord(player);
    }

    private static final class TestMessages extends CoreMessages {

        private TestMessages() {
            super(null);
        }

        @Override
        public Component component(String key, String fallback) {
            return Component.empty();
        }

        @Override
        public Component component(String key, String fallback, Map<String, String> placeholders) {
            return Component.empty();
        }

        @Override
        public List<Component> list(String key, List<String> fallback) {
            return List.of(Component.text("help"));
        }
    }
}

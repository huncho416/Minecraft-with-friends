package net.mythicpvp.suite.command;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandBlockerTest {

    @Test
    void hidesBlockedAndNoPermissionCommandsFromRootCompletion() {
        CommandManager manager = mock(CommandManager.class);
        Player player = mock(Player.class);
        when(manager.getPermission("admin")).thenReturn("mythic.admin");
        when(player.hasPermission("mythic.admin")).thenReturn(false);
        CommandBlocker blocker = new CommandBlocker(manager, config(Set.of("pl"), Map.of()));

        List<String> completions = blocker.filterCompletions(player, "/", List.of("spawn", "pl", "admin"));

        assertEquals(List.of("spawn"), completions);
    }

    @Test
    void allowsConfiguredBlockedCommandWhenRequiredPermissionMatches() {
        CommandManager manager = mock(CommandManager.class);
        Player player = mock(Player.class);
        when(player.hasPermission("mythic.admin.commands.plugins")).thenReturn(true);
        CommandBlocker blocker = new CommandBlocker(manager, config(Set.of("pl"), Map.of("pl", "mythic.admin.commands.plugins")));

        assertTrue(blocker.canUse(player, "/pl"));
        assertEquals(List.of("pl"), blocker.filterCompletions(player, "/", List.of("pl")));
    }

    @Test
    void deniesConfiguredBlockedCommandWithoutPermission() {
        CommandManager manager = mock(CommandManager.class);
        Player player = mock(Player.class);
        when(player.hasPermission("mythic.admin.commands.help")).thenReturn(false);
        CommandBlocker blocker = new CommandBlocker(manager, config(Set.of("?"), Map.of("?", "mythic.admin.commands.help")));

        assertFalse(blocker.canUse(player, "/?"));
        assertEquals(List.of("spawn"), blocker.filterCommands(player, List.of("?", "spawn")));
    }

    private static CommandBlockerConfig config(Set<String> blocked, Map<String, String> permissions) {
        return new CommandBlockerConfig(true, true, "mythic.commandblocker.bypass", "blocked", blocked, permissions);
    }
}

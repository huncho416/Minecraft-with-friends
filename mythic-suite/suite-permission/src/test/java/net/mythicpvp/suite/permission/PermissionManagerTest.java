package net.mythicpvp.suite.permission;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionManagerTest {

    @Test
    void resolvesInheritanceAndWildcards() {
        PermissionManager manager = PermissionManager.getInstance();
        manager.registerRank(new Rank("default", "&7", "#808080", 100, Set.of("hub.join"), null));
        manager.registerRank(new Rank("admin", "&c", "#FF0000", 1, Set.of("mythic.*"), "default"));
        UUID player = UUID.randomUUID();
        manager.setPlayerRank(player, "admin");
        assertTrue(manager.hasPermission(player, "hub.join"));
        assertTrue(manager.hasPermission(player, "mythic.command.reload"));
        assertFalse(manager.hasPermission(player, "skyblock.admin"));
    }
}

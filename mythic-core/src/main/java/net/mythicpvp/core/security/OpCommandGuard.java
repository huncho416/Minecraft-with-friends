package net.mythicpvp.core.security;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

public final class OpCommandGuard implements Listener {

    private static final Set<String> BLOCKED = Set.of(
            "op", "deop",
            "minecraft:op", "minecraft:deop",
            "bukkit:op", "bukkit:deop");

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String command = parseCommand(event.getMessage());
        if (!BLOCKED.contains(command)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.sendMessage(MythicHex.colorize(
                "&#FF8A8A/op and /deop are console-only."));
    }

    @EventHandler
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
    }

    @NotNull
    private static String parseCommand(@NotNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        if (space >= 0) {
            trimmed = trimmed.substring(0, space);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}

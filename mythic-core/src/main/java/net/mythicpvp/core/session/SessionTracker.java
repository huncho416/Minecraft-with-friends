package net.mythicpvp.core.session;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionTracker implements Listener {

    private final Map<UUID, Long> loginAtMillis = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        loginAtMillis.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        loginAtMillis.remove(event.getPlayer().getUniqueId());
    }

    public long loginTime(@NotNull Player player) {
        return loginAtMillis.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
    }
}

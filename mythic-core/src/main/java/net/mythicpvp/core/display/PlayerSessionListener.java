package net.mythicpvp.core.display;

import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerSessionListener implements Listener {

    private final DisplayService display;

    public PlayerSessionListener(@NotNull DisplayService display) {
        this.display = display;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(@NotNull PlayerJoinEvent event) {

        display.apply(event.getPlayer());

        MythicScheduler.runSync(display.plugin(), display::applyAll);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        display.clear(event.getPlayer());

        MythicScheduler.runSync(display.plugin(), display::applyAll);
    }
}

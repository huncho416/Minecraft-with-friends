package net.mythicpvp.core.display;

import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges Bukkit player lifecycle events into the {@link DisplayService}.
 *
 * <p>On join: push the full display state (tab, nametag, scoreboard) plus
 * trigger an {@code applyAll} on the next tick so every existing player's
 * tab list shows the new arrival with the correct sort order.
 *
 * <p>On quit: clear local display state and refresh other players' tabs
 * so the leaver disappears.
 *
 * <p>Priority is {@link EventPriority#MONITOR} for joins so other plugins
 * have already had their say about the player's name / display before we
 * compute the rank-driven prefix.
 */
public final class PlayerSessionListener implements Listener {

    private final DisplayService display;

    public PlayerSessionListener(@NotNull DisplayService display) {
        this.display = display;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        // Apply the joiner immediately so they see their tab/scoreboard
        // on the first frame.
        display.apply(event.getPlayer());
        // Then refresh everyone else so the joiner appears in their tab
        // and the %online% counter advances. Folia-safe via MythicScheduler.
        MythicScheduler.runSync(display.plugin(), display::applyAll);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        display.clear(event.getPlayer());
        // Refresh remaining players so the leaver drops from tab and
        // %online% decrements. Scheduled for next tick because at this
        // event firing the leaver still appears in getOnlinePlayers().
        MythicScheduler.runSync(display.plugin(), display::applyAll);
    }
}

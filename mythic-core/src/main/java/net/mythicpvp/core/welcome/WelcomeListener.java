package net.mythicpvp.core.welcome;

import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class WelcomeListener implements Listener {

    private final JavaPlugin plugin;
    private final WelcomeService welcome;

    public WelcomeListener(@NotNull JavaPlugin plugin, @NotNull WelcomeService welcome) {
        this.plugin = plugin;
        this.welcome = welcome;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (!welcome.enabled()) {
            return;
        }
        MythicScheduler.runLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                welcome.send(event.getPlayer());
            }
        }, 10L);
    }
}

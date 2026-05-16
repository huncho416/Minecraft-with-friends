package net.mythicpvp.core.maintenance;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

public final class MaintenanceLoginGuard implements Listener {

    private final MaintenanceService maintenance;
    private final CoreMessages messages;

    public MaintenanceLoginGuard(@NotNull MaintenanceService maintenance, @NotNull CoreMessages messages) {
        this.maintenance = maintenance;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (!maintenance.isActive()) {
            return;
        }
        if (maintenance.canBypass(event.getUniqueId())) {
            return;
        }
        Component reason = messages.component(
                "messages.maintenance.kick",
                "<#FF8A8A><bold>This server is in maintenance mode.\n\n"
                + "<white>Join our Discord to keep up with the latest updates:\n"
                + "<#9CC3FF><click:open_url:'https://discord.gg/mythicpvp'>discord.gg/mythicpvp</click>");
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, reason);
    }
}

package net.mythicpvp.core.maintenance;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class MaintenanceLoginGuard implements Listener {

    private final MaintenanceService maintenance;
    private final CoreMessages messages;

    public MaintenanceLoginGuard(@NotNull MaintenanceService maintenance, @NotNull CoreMessages messages) {
        this.maintenance = maintenance;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (!maintenance.isActive()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(MaintenanceService.BYPASS_PERMISSION)) {
            return;
        }
        if (maintenance.canBypass(player.getUniqueId())) {
            return;
        }
        Component reason = messages.component(
                "messages.maintenance.kick",
                "<red><bold>This server is in maintenance mode.\n\n"
                + "<white>Join our Discord to keep up with the latest updates:\n"
                + "<#9CC3FF><click:open_url:'https://discord.gg/mythicpvp'>discord.gg/mythicpvp</click>");
        player.kick(reason);
    }
}

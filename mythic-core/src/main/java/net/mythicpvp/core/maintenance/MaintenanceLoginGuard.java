package net.mythicpvp.core.maintenance;

import net.kyori.adventure.text.Component;
import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
        if (isOperator(event.getUniqueId())) {
            return;
        }
        Component reason = messages.codeOwned(
                "<#FF8A8A><bold>This server is in maintenance mode.</bold></#FF8A8A>\n\n"
                + "<white>Join our Discord to keep up with the latest updates:</white>\n"
                + "<hover:show_text:'<#9CC3FF>Click to open Discord'><click:open_url:'https://discord.gg/mythicpvp'><#9CC3FF><underlined>discord.gg/mythicpvp</underlined></#9CC3FF></click></hover>");
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, reason);
    }

    private static boolean isOperator(@NotNull UUID uuid) {
        try {
            for (OfflinePlayer op : Bukkit.getOperators()) {
                if (uuid.equals(op.getUniqueId())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}

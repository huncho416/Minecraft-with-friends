package net.mythicpvp.core.session;

import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.schema.MythicSchema;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class SessionPresenceListener implements Listener {

    private final String localShardId;

    public SessionPresenceListener(@NotNull String localShardId) {
        this.localShardId = localShardId;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            return;
        }
        MythicSchema schema = new MythicSchema(connection);
        schema.sessionLogin(event.getPlayer().getUniqueId(), event.getPlayer().getName(),
                localShardId, 0L, "", "");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            return;
        }
        MythicSchema schema = new MythicSchema(connection);
        schema.sessionLogout(event.getPlayer().getUniqueId(), "quit");
    }
}

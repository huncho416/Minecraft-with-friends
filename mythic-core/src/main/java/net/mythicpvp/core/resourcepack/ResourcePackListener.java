package net.mythicpvp.core.resourcepack;

import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.resourcepack.ResourcePackManager;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ResourcePackListener implements Listener {

    private final JavaPlugin plugin;
    private final boolean sendOnJoin;
    private final long sendDelayTicks;

    public ResourcePackListener(@NotNull JavaPlugin plugin,
                                 boolean sendOnJoin,
                                 long sendDelayTicks) {
        this.plugin = plugin;
        this.sendOnJoin = sendOnJoin;
        this.sendDelayTicks = sendDelayTicks;
    }

    public static void configure(@NotNull JavaPlugin plugin, @NotNull MythicConfig config) {
        boolean enabled = config.getBoolean("resource-pack.enabled", false);
        if (!enabled) {
            plugin.getLogger().info("[resource-pack] disabled in resourcepack.yml");
            return;
        }
        String url = config.getString("resource-pack.url", "");
        String sha1 = config.getString("resource-pack.sha1", "");
        if (url == null || url.isBlank()) {
            plugin.getLogger().warning("[resource-pack] enabled=true but url is blank — skipping");
            return;
        }
        boolean force = config.getBoolean("resource-pack.force", false);
        boolean sendOnJoin = config.getBoolean("resource-pack.send-on-join", true);
        long sendDelay = config.getInt("resource-pack.send-delay-ticks", 40);

        ResourcePackManager manager = ResourcePackManager.getInstance();
        manager.setPackInfo(url, sha1 == null ? "" : sha1.toLowerCase().trim());
        manager.setForceUpdate(force);
        plugin.getLogger().info("[resource-pack] configured: url=" + url + " sha1=" + sha1 + " force=" + force);

        if (sendOnJoin) {
            plugin.getServer().getPluginManager().registerEvents(
                    new ResourcePackListener(plugin, true, sendDelay), plugin);
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (!sendOnJoin) return;
        Player player = event.getPlayer();
        MythicScheduler.runLater(plugin, () -> {
            if (!player.isOnline()) return;
            try {
                ResourcePackManager.getInstance().sendTo(player);
            } catch (Throwable t) {
                plugin.getLogger().warning("[resource-pack] sendTo " + player.getName()
                        + " failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }, sendDelayTicks);
    }
}

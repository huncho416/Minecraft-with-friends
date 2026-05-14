package net.mythicpvp.hub.activity;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.jetbrains.annotations.NotNull;

public final class HubActivityListener implements Listener {

    private final HubActivityService activityService;

    public HubActivityListener(@NotNull HubActivityService activityService) {
        this.activityService = activityService;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        activityService.enableFlight(event.getPlayer());
    }

    @EventHandler
    public void onToggleFlight(@NotNull PlayerToggleFlightEvent event) {
        if (!activityService.isDoubleJumpEnabled()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        activityService.applyDoubleJump(player);
    }

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!activityService.isLaunchPadsEnabled()) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Block below = player.getLocation().subtract(0, 1, 0).getBlock();
        Material padMat = activityService.getLaunchPadMaterial();
        if (below.getType() == padMat) {
            activityService.applyLaunchPad(player);
        }
    }
}

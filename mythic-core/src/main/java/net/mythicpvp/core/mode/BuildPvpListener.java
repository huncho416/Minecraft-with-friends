package net.mythicpvp.core.mode;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class BuildPvpListener implements Listener {

    public static final String BUILD_BYPASS = "mythic.core.build";
    public static final String PVP_BYPASS = "mythic.core.pvp";

    private final PlayerModeService modes;

    public BuildPvpListener(@NotNull PlayerModeService modes) {
        this.modes = modes;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        if (allowedBuild(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        if (allowedBuild(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.PHYSICAL) return;
        if (allowedBuild(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        Player victim = event.getEntity() instanceof Player p ? p : null;
        if (attacker == null || victim == null) return;
        if (allowedPvp(attacker) && allowedPvp(victim)) return;
        event.setCancelled(true);
        attacker.sendMessage(MythicHex.colorize(
                "&#FF8A8APvP is disabled. Use &f/pvpmode &#FF8A8Ato toggle."));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        modes.clear(event.getPlayer().getUniqueId());
    }

    private boolean allowedBuild(@NotNull Player player) {
        return player.hasPermission(BUILD_BYPASS) && modes.isBuilder(player);
    }

    private boolean allowedPvp(@NotNull Player player) {
        return player.hasPermission(PVP_BYPASS) && modes.isPvp(player);
    }

    private Player resolveAttacker(@NotNull Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player p) return p;
        return null;
    }
}

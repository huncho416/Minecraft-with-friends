package net.mythicpvp.core.world;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class SkyblockWorldRules implements Listener {

    private final JavaPlugin plugin;

    public SkyblockWorldRules(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(@NotNull WorldInitEvent event) {
        applyRules(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(@NotNull WorldLoadEvent event) {
        applyRules(event.getWorld());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(@NotNull CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL
                || reason == CreatureSpawnEvent.SpawnReason.JOCKEY
                || reason == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                || reason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION
                || reason == CreatureSpawnEvent.SpawnReason.PATROL
                || reason == CreatureSpawnEvent.SpawnReason.RAID) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onWeather(@NotNull WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        event.joinMessage(null);
    }

    public void applyAll() {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                applyRulesUnsafe(world);
            }
        });
    }

    private void applyRules(@NotNull World world) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> applyRulesUnsafe(world));
    }

    @SuppressWarnings("removal")
    private void applyRulesUnsafe(@NotNull World world) {
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    }
}

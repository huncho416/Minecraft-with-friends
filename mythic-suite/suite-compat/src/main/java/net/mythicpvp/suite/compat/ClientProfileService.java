package net.mythicpvp.suite.compat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientProfileService implements Listener {

    private final Map<UUID, ClientProfile> profiles = new ConcurrentHashMap<>();
    private final FloodgateBridge floodgate = new FloodgateBridge();

    @NotNull
    public ClientProfile profileFor(@NotNull Player player) {
        ClientProfile cached = profiles.get(player.getUniqueId());
        if (cached != null) return cached;
        ClientProfile fresh = build(player);
        profiles.put(player.getUniqueId(), fresh);
        return fresh;
    }

    @NotNull
    public ClientProfile profileFor(@NotNull UUID uuid) {
        ClientProfile cached = profiles.get(uuid);
        return cached != null ? cached : ClientProfile.UNKNOWN_MODERN;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(@NotNull PlayerLoginEvent event) {
        Player player = event.getPlayer();
        profiles.put(player.getUniqueId(), build(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profiles.computeIfAbsent(player.getUniqueId(), k -> build(player));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        profiles.remove(event.getPlayer().getUniqueId());
    }

    @NotNull
    private ClientProfile build(@NotNull Player player) {
        int protocol = safeProtocol(player);
        boolean bedrock = floodgate.isBedrock(player);
        ProfileTier tier = ClientProfile.tierFor(protocol, bedrock);
        return new ClientProfile(player.getUniqueId(), protocol, bedrock, tier);
    }

    private static int safeProtocol(@NotNull Player player) {
        try {
            return player.getProtocolVersion();
        } catch (Throwable ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static final class FloodgateBridge {
        private volatile Boolean available;
        private volatile Method apiGetInstance;
        private volatile Method apiIsFloodgatePlayer;
        private volatile Object api;

        boolean isBedrock(@NotNull Player player) {
            if (player.getName().startsWith(".")) return true;
            if (Boolean.FALSE.equals(available)) return false;
            try {
                if (api == null) {
                    Class<?> apiCls = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                    apiGetInstance = apiCls.getMethod("getInstance");
                    apiIsFloodgatePlayer = apiCls.getMethod("isFloodgatePlayer", UUID.class);
                    api = apiGetInstance.invoke(null);
                    available = true;
                }
                Object result = apiIsFloodgatePlayer.invoke(api, player.getUniqueId());
                return result instanceof Boolean b && b;
            } catch (Throwable ignored) {
                available = false;
                return false;
            }
        }
    }
}

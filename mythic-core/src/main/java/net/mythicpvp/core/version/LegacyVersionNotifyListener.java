package net.mythicpvp.core.version;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LegacyVersionNotifyListener implements Listener {

    private static final int RECOMMENDED_PROTOCOL = 767;

    private final JavaPlugin plugin;
    private final Set<UUID> notifiedThisSession = ConcurrentHashMap.newKeySet();

    public LegacyVersionNotifyListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!notifiedThisSession.add(uuid)) {
            return;
        }
        int protocol = safeProtocol(player);
        if (protocol <= 0 || protocol >= RECOMMENDED_PROTOCOL) {
            return;
        }
        MythicScheduler.runLater(plugin, () -> sendRecommendation(player, protocol), 40L);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        notifiedThisSession.remove(event.getPlayer().getUniqueId());
    }

    private void sendRecommendation(@NotNull Player player, int protocol) {
        if (!player.isOnline()) return;
        String detected = describeProtocol(protocol);
        Component blank = Component.empty();
        player.sendMessage(blank);
        player.sendMessage(MythicHex.colorize("&#FF8A8A&l&m                                                  "));
        player.sendMessage(MythicHex.colorize("&#FF8A8A&lLEGACY CLIENT DETECTED &8» &#FFFFFF" + detected));
        player.sendMessage(blank);
        player.sendMessage(MythicHex.colorize(
                "&#FFFFFFIt looks like you're on an older Minecraft version. We"));
        player.sendMessage(MythicHex.colorize(
                "&#FFFFFFrecommend updating to &#FFD700&l1.21+ &#FFFFFFfor the best experience"));
        player.sendMessage(MythicHex.colorize(
                "&#FFFFFFon MythicPvP &#D2D8E0(menus, animations, and chat tags render"));
        player.sendMessage(MythicHex.colorize(
                "&#FFFFFFat full quality on the latest version)."));
        player.sendMessage(blank);
        player.sendMessage(MythicHex.colorize(
                "&#D2D8E0You'll still be able to play normally on your current version."));
        player.sendMessage(MythicHex.colorize("&#FF8A8A&l&m                                                  "));
        player.sendMessage(blank);
    }

    private static int safeProtocol(@NotNull Player player) {
        try {
            return player.getProtocolVersion();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    @NotNull
    private static String describeProtocol(int protocol) {
        if (protocol >= 770) return "Java 1.21.5+";
        if (protocol >= 769) return "Java 1.21.4";
        if (protocol >= 768) return "Java 1.21.3";
        if (protocol >= 767) return "Java 1.21";
        if (protocol >= 766) return "Java 1.20.5/1.20.6";
        if (protocol >= 765) return "Java 1.20.3/1.20.4";
        if (protocol >= 764) return "Java 1.20.2";
        if (protocol >= 763) return "Java 1.20/1.20.1";
        if (protocol >= 762) return "Java 1.19.4";
        if (protocol >= 761) return "Java 1.19.3";
        if (protocol >= 760) return "Java 1.19.1/1.19.2";
        if (protocol >= 759) return "Java 1.19";
        if (protocol >= 758) return "Java 1.18.2";
        if (protocol >= 757) return "Java 1.18/1.18.1";
        if (protocol >= 756) return "Java 1.17.1";
        if (protocol >= 755) return "Java 1.17";
        if (protocol >= 754) return "Java 1.16.5";
        if (protocol >= 751) return "Java 1.16.4";
        if (protocol >= 736) return "Java 1.16.x";
        if (protocol >= 578) return "Java 1.15.x";
        if (protocol >= 498) return "Java 1.14.x";
        if (protocol >= 401) return "Java 1.13.x";
        return "Legacy Java";
    }
}

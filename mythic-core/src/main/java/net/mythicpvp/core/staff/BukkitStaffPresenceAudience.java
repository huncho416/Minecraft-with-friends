package net.mythicpvp.core.staff;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class BukkitStaffPresenceAudience implements Consumer<StaffPresenceEvent> {

    private static final long SWITCH_WINDOW_MILLIS = 5_000L;
    private static final long SWITCH_WINDOW_TICKS = SWITCH_WINDOW_MILLIS / 50 + 5;

    private final JavaPlugin plugin;
    private final Map<UUID, PendingQuit> pendingQuits = new ConcurrentHashMap<>();

    public BukkitStaffPresenceAudience(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void accept(@NotNull StaffPresenceEvent event) {
        StaffPresenceEvent effective = mergeIntoSwitch(event);
        if (effective == null) {
            return;
        }
        String template = templateFor(effective.type());
        String rendered = interpolate(template, Map.of(
                "server", effective.server(),
                "sender", effective.staffName(),
                "rank", effective.rank(),
                "rank_color", coerceColor(effective.rankColor()),
                "from", effective.fromServer(),
                "to", effective.toServer()));
        Component component = MythicHex.colorize(rendered);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    private StaffPresenceEvent mergeIntoSwitch(@NotNull StaffPresenceEvent incoming) {
        long now = System.currentTimeMillis();
        pendingQuits.entrySet().removeIf(e -> now - e.getValue().timestamp > SWITCH_WINDOW_MILLIS);
        switch (incoming.type()) {
            case QUIT -> {
                pendingQuits.put(incoming.staffUuid(),
                        new PendingQuit(incoming.server(), incoming.staffName(), incoming.rank(), incoming.rankColor(), now));
                MythicScheduler.runLater(plugin,
                        () -> emitDeferredQuit(incoming.staffUuid(), now),
                        SWITCH_WINDOW_TICKS);
                return null;
            }
            case JOIN -> {
                PendingQuit pending = pendingQuits.remove(incoming.staffUuid());
                if (pending != null && !pending.server.equalsIgnoreCase(incoming.server())) {
                    return new StaffPresenceEvent(
                            StaffPresenceType.SWITCH,
                            incoming.server(),
                            incoming.staffUuid(),
                            incoming.staffName(),
                            incoming.rank(),
                            incoming.rankColor(),
                            pending.server,
                            incoming.server(),
                            now);
                }
                return incoming;
            }
            default -> {
                return incoming;
            }
        }
    }

    private void emitDeferredQuit(@NotNull UUID staffUuid, long quitTimestamp) {
        PendingQuit pending = pendingQuits.remove(staffUuid);
        if (pending == null || pending.timestamp != quitTimestamp) {
            return;
        }
        StaffPresenceEvent event = new StaffPresenceEvent(
                StaffPresenceType.QUIT,
                pending.server,
                staffUuid,
                pending.staffName,
                pending.rank,
                pending.rankColor,
                pending.server,
                "",
                System.currentTimeMillis());
        renderQuitDirect(event);
    }

    private void renderQuitDirect(@NotNull StaffPresenceEvent event) {
        String rendered = interpolate(templateFor(StaffPresenceType.QUIT), Map.of(
                "server", event.server(),
                "sender", event.staffName(),
                "rank", event.rank(),
                "rank_color", coerceColor(event.rankColor()),
                "from", event.fromServer(),
                "to", event.toServer()));
        Component component = MythicHex.colorize(rendered);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    @NotNull
    private String templateFor(@NotNull StaffPresenceType type) {
        return switch (type) {
            case JOIN -> "&#9CC3FF[S] %rank_color%%sender% &#9CFF9Cjoined &#FFFFFF%server%&#9CFF9C.";
            case QUIT -> "&#9CC3FF[S] %rank_color%%sender% &#FF8A8Aleft &#FFFFFF%server%&#FF8A8A.";
            case SWITCH -> "&#9CC3FF[S] %rank_color%%sender% &7switched from &#FFFFFF%from% &7to &#FFFFFF%to%&7.";
        };
    }

    @NotNull
    private static String coerceColor(@NotNull String raw) {
        if (raw.isBlank()) {
            return "&7";
        }
        if (raw.startsWith("#") && !raw.startsWith("&#")) {
            return "&" + raw;
        }
        return raw;
    }

    @NotNull
    private static String interpolate(@NotNull String template, @NotNull Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private record PendingQuit(@NotNull String server, @NotNull String staffName,
                               @NotNull String rank, @NotNull String rankColor, long timestamp) {}
}

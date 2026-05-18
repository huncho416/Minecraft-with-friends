package net.mythicpvp.core.session;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.staff.StaffPresenceListener;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CrossShardPresenceService {

    private final RankService ranks;
    private final GrantService grants;
    private final String localShardId;
    private final Logger logger;
    private final Map<UUID, SessionSnapshot> previous = new HashMap<>();
    private volatile boolean baselineDone = false;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-presence-poll");
        t.setDaemon(true);
        return t;
    });

    public CrossShardPresenceService(@NotNull RankService ranks, @NotNull GrantService grants,
                                      @NotNull String localShardId, @NotNull Logger logger) {
        this.ranks = ranks;
        this.grants = grants;
        this.localShardId = localShardId;
        this.logger = logger;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[presence] no STDB connection; cross-shard staff notices disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 4, 3, TimeUnit.SECONDS);
        logger.info("[presence] polling sessions every 3s for staff cross-shard notices");
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM sessions").thenAccept(body -> {
            try {
                apply(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[presence] poll parse failed", e);
            }
        });
    }

    private void apply(@NotNull String body) {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException e) {
            return;
        }
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) return;
        JsonObject table = root.getAsJsonArray().get(0).getAsJsonObject();
        if (!table.has("rows")) return;
        JsonArray rows = table.getAsJsonArray("rows");

        Map<UUID, SessionSnapshot> current = new HashMap<>();
        for (JsonElement rowEl : rows) {
            if (!rowEl.isJsonArray()) continue;
            JsonArray row = rowEl.getAsJsonArray();
            if (row.size() < 3) continue;
            UUID uuid;
            try {
                uuid = UUID.fromString(row.get(0).getAsString());
            } catch (RuntimeException e) {
                continue;
            }
            String username = row.get(1).getAsString();
            String shardId = row.get(2).getAsString();
            current.put(uuid, new SessionSnapshot(username, shardId));
        }

        if (!baselineDone) {
            previous.putAll(current);
            baselineDone = true;
            return;
        }

        Set<UUID> all = new HashSet<>(previous.keySet());
        all.addAll(current.keySet());
        for (UUID uuid : all) {
            SessionSnapshot before = previous.get(uuid);
            SessionSnapshot after = current.get(uuid);
            if (before == null && after != null) {
                notifyJoin(uuid, after);
            } else if (before != null && after == null) {
                notifyQuit(uuid, before);
            } else if (before != null && !before.shardId.equalsIgnoreCase(after.shardId)) {
                notifyMove(uuid, before, after);
            }
        }
        previous.clear();
        previous.putAll(current);
    }

    private void notifyJoin(@NotNull UUID uuid, @NotNull SessionSnapshot snap) {
        if (snap.shardId.equalsIgnoreCase(localShardId)) return;
        StaffLabel label = labelFor(uuid, snap.username);
        if (label == null) return;
        broadcast("&#9CC3FF[S] " + label.colorTag + snap.username + " &#9CFF9Cjoined &#FFFFFF" + snap.shardId + "&#9CFF9C.");
    }

    private void notifyQuit(@NotNull UUID uuid, @NotNull SessionSnapshot snap) {
        if (snap.shardId.equalsIgnoreCase(localShardId)) return;
        StaffLabel label = labelFor(uuid, snap.username);
        if (label == null) return;
        broadcast("&#9CC3FF[S] " + label.colorTag + snap.username + " &#FF8A8Aleft &#FFFFFF" + snap.shardId + "&#FF8A8A.");
    }

    private void notifyMove(@NotNull UUID uuid, @NotNull SessionSnapshot before, @NotNull SessionSnapshot after) {
        StaffLabel label = labelFor(uuid, after.username);
        if (label == null) return;
        broadcast("&#9CC3FF[S] " + label.colorTag + after.username
                + " &7switched from &#FFFFFF" + before.shardId + " &7to &#FFFFFF" + after.shardId + "&7.");
    }

    private void broadcast(@NotNull String message) {
        var component = MythicHex.colorize(message);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    private StaffLabel labelFor(@NotNull UUID uuid, @NotNull String username) {
        String rankId = grants.activeRank(uuid);
        CoreRank rank = ranks.get(rankId);
        if (rank == null || !rank.staff()) return null;
        return new StaffLabel(net.mythicpvp.core.rank.PlayerNameColor.miniColor(rank.color()));
    }

    public String shardOf(@NotNull String username) {
        for (SessionSnapshot snap : previous.values()) {
            if (snap.username.equalsIgnoreCase(username)) {
                return snap.shardId;
            }
        }
        return null;
    }

    private record SessionSnapshot(@NotNull String username, @NotNull String shardId) {}

    private record StaffLabel(@NotNull String colorTag) {}
}

package net.mythicpvp.hub.selector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.TableEvent;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.ServerEntryRow;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RegistrySubscriber {

    private final JavaPlugin plugin;
    private final ServerSelectorService selectorService;
    private final Logger logger;
    private final Set<String> seenShards = new ConcurrentSkipListSet<>();
    private final ScheduledExecutorService refreshExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mythic-hub-registry-refresh");
                t.setDaemon(true);
                return t;
            });

    public RegistrySubscriber(@NotNull JavaPlugin plugin, @NotNull ServerSelectorService selectorService) {
        this.plugin = plugin;
        this.selectorService = selectorService;
        this.logger = plugin.getLogger();
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.warning("[selector] no STDB connection available; selector will only show locally-known servers");
            return;
        }
        connection.subscribeTable(TableNames.SERVER_REGISTRY, this::handleEvent);
        logger.info("[selector] subscribed to " + TableNames.SERVER_REGISTRY);
        refreshExecutor.scheduleAtFixedRate(() -> {
            try {
                pollViaSql(connection);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[selector] poll failed", e);
            }
        }, 2, 5, TimeUnit.SECONDS);
    }

    private void handleEvent(@NotNull TableEvent event) {
        ServerEntryRow row = StdbRowParser.parse(event.payload(), ServerEntryRow.class);
        if (row == null || row.shard_id() == null) {
            return;
        }
        if ("delete".equalsIgnoreCase(event.operation())) {
            seenShards.remove(row.shard_id());
            MythicScheduler.runSync(plugin,
                    () -> selectorService.removeServer(row.shard_id()));
            return;
        }
        boolean healthy = "HEALTHY".equalsIgnoreCase(row.status());
        if (healthy) {
            seenShards.add(row.shard_id());
        }
        MythicScheduler.runSync(plugin, () ->
                selectorService.updateServer(row.shard_id(), row.role(), row.player_count(), row.tps(), healthy));
    }

    private void pollViaSql(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.SERVER_REGISTRY).thenAccept(body -> {
            try {
                applySqlSnapshot(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[selector] snapshot parse failed", e);
            }
        });
    }

    private void applySqlSnapshot(@NotNull String body) {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException e) {
            return;
        }
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            return;
        }
        JsonObject table = root.getAsJsonArray().get(0).getAsJsonObject();
        if (!table.has("rows")) {
            return;
        }
        JsonArray rows = table.getAsJsonArray("rows");
        Set<String> snapshotIds = new HashSet<>();
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonArray()) continue;
            ServerEntryRow row = StdbRowParser.parse(rowElement.toString(), ServerEntryRow.class);
            if (row == null || row.shard_id() == null) continue;
            snapshotIds.add(row.shard_id());
            boolean healthy = "HEALTHY".equalsIgnoreCase(row.status());
            MythicScheduler.runSync(plugin, () ->
                    selectorService.updateServer(row.shard_id(), row.role(), row.player_count(), row.tps(), healthy));
        }
        Set<String> stale = new HashSet<>(seenShards);
        stale.removeAll(snapshotIds);
        for (String shardId : stale) {
            seenShards.remove(shardId);
            MythicScheduler.runSync(plugin, () -> selectorService.removeServer(shardId));
        }
        seenShards.addAll(snapshotIds);
    }
}

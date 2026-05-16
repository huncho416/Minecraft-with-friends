package net.mythicpvp.core.transfer;

import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.TableEvent;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.ServerEntryRow;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory cache of shards known to the cluster, populated by subscribing
 * to the STDB {@code server_registry} table. Provides cheap reads for
 * tab-completion + admin commands without re-querying STDB each time.
 *
 * <p>Mirrors the same subscription pattern used in {@link
 * net.mythicpvp.core.command.HubCommand} but exposes the data globally
 * rather than scoped to HUB-only entries.
 */
public final class ShardRegistry {

    private static final String HEALTHY = "HEALTHY";

    private final Map<String, ServerEntryRow> shards = new ConcurrentHashMap<>();
    private final Logger logger;
    private final ScheduledExecutorService refreshExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mythic-shard-registry-refresh");
                t.setDaemon(true);
                return t;
            });

    public ShardRegistry(@NotNull Logger logger) {
        this.logger = logger;
    }

    public void subscribe() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[shard-registry] no STDB connection; tab-completion will be empty");
            return;
        }
        connection.subscribeTable(TableNames.SERVER_REGISTRY, this::handleEvent);
        logger.info("[shard-registry] subscribed to " + TableNames.SERVER_REGISTRY);
        refreshExecutor.scheduleAtFixedRate(() -> {
            try {
                connection.subscribeTable(TableNames.SERVER_REGISTRY, this::handleEvent);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[shard-registry] re-subscribe failed", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void handleEvent(@NotNull TableEvent event) {
        ServerEntryRow row = StdbRowParser.parse(event.payload(), ServerEntryRow.class);
        if (row == null || row.shard_id() == null) {
            return;
        }
        if ("delete".equalsIgnoreCase(event.operation())
                || !HEALTHY.equalsIgnoreCase(row.status())) {
            shards.remove(row.shard_id());
            return;
        }
        shards.put(row.shard_id(), row);
    }

    @NotNull
    public List<String> shardIds() {
        List<String> out = new ArrayList<>(shards.keySet());
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    @NotNull
    public List<String> shardIdsForRole(@NotNull String role) {
        return shards.values().stream()
                .filter(r -> role.equalsIgnoreCase(r.role()))
                .map(ServerEntryRow::shard_id)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @NotNull
    public List<ServerEntryRow> all() {
        return shards.values().stream()
                .sorted(Comparator.comparing(ServerEntryRow::shard_id, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}

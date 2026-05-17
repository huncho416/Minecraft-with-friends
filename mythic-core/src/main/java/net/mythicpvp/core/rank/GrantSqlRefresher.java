package net.mythicpvp.core.rank;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.core.persistence.StdbPersistenceGateway;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.RankGrantRow;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GrantSqlRefresher {

    private final GrantService grants;
    private final Logger logger;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-grant-refresh");
        t.setDaemon(true);
        return t;
    });

    public GrantSqlRefresher(@NotNull GrantService grants, @NotNull Logger logger) {
        this.grants = grants;
        this.logger = logger;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[grant-refresh] no STDB connection; cross-shard rank sync disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 2, 5, TimeUnit.SECONDS);
        logger.info("[grant-refresh] polling rank_grants every 5s");
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.RANK_GRANTS).thenAccept(body -> {
            try {
                apply(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[grant-refresh] parse failed", e);
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
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            return;
        }
        JsonObject table = root.getAsJsonArray().get(0).getAsJsonObject();
        if (!table.has("rows")) {
            return;
        }
        JsonArray rows = table.getAsJsonArray("rows");
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonArray()) continue;
            RankGrantRow row = StdbRowParser.parse(rowElement.toString(), RankGrantRow.class);
            if (row == null) continue;
            RankGrant grant = StdbPersistenceGateway.toRankGrant(row);
            grants.applyGrant(grant);
        }
    }
}

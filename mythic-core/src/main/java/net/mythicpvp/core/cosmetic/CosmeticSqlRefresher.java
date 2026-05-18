package net.mythicpvp.core.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.core.persistence.HydrationSink;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.CosmeticGrantRow;
import net.mythicpvp.suite.database.schema.dto.EquippedSlotRow;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CosmeticSqlRefresher {

    private final HydrationSink sink;
    private final Logger logger;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-cosmetic-refresh");
        t.setDaemon(true);
        return t;
    });

    public CosmeticSqlRefresher(@NotNull HydrationSink sink, @NotNull Logger logger) {
        this.sink = sink;
        this.logger = logger;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[cosmetic-refresh] no STDB connection; cross-shard cosmetic sync disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 2, 5, TimeUnit.SECONDS);
        logger.info("[cosmetic-refresh] polling cosmetic_grants + cosmetic_equipped every 5s");
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.COSMETIC_GRANTS).thenAccept(body -> {
            try {
                applyGrants(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[cosmetic-refresh] grants parse failed", e);
            }
        });
        connection.sql("SELECT * FROM " + TableNames.COSMETIC_EQUIPPED).thenAccept(body -> {
            try {
                applyEquipped(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[cosmetic-refresh] equipped parse failed", e);
            }
        });
    }

    private void applyGrants(@NotNull String body) {
        JsonArray rows = extractRows(body);
        if (rows == null) return;
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonArray()) continue;
            CosmeticGrantRow row = StdbRowParser.parse(rowElement.toString(), CosmeticGrantRow.class);
            if (row == null || row.player_uuid() == null) continue;
            UUID player;
            try {
                player = UUID.fromString(row.player_uuid());
            } catch (IllegalArgumentException e) {
                continue;
            }
            sink.applyCosmeticGrant(player, row.cosmetic_id(), row.cosmetic_type());
        }
    }

    private void applyEquipped(@NotNull String body) {
        JsonArray rows = extractRows(body);
        if (rows == null) return;
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonArray()) continue;
            EquippedSlotRow row = StdbRowParser.parse(rowElement.toString(), EquippedSlotRow.class);
            if (row == null || row.player_uuid() == null) continue;
            UUID player;
            try {
                player = UUID.fromString(row.player_uuid());
            } catch (IllegalArgumentException e) {
                continue;
            }
            sink.applyCosmeticEquip(player, row.cosmetic_type(), row.cosmetic_id());
        }
    }

    private static JsonArray extractRows(@NotNull String body) {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException e) {
            return null;
        }
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) return null;
        JsonObject table = root.getAsJsonArray().get(0).getAsJsonObject();
        if (!table.has("rows")) return null;
        return table.getAsJsonArray("rows");
    }
}

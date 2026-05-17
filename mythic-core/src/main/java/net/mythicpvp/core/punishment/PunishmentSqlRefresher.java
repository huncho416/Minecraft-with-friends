package net.mythicpvp.core.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.core.persistence.StdbPersistenceGateway;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.PunishmentRow;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PunishmentSqlRefresher {

    private final PunishmentService punishments;
    private final Logger logger;
    private final Map<Long, Boolean> wasActive = new HashMap<>();
    private volatile boolean firstPollComplete = false;
    private volatile Consumer<PunishmentRecord> remoteEnforcer = record -> {};
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-punishment-refresh");
        t.setDaemon(true);
        return t;
    });

    public PunishmentSqlRefresher(@NotNull PunishmentService punishments, @NotNull Logger logger) {
        this.punishments = punishments;
        this.logger = logger;
    }

    public void setRemoteEnforcer(@NotNull Consumer<PunishmentRecord> enforcer) {
        this.remoteEnforcer = enforcer;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[punishment-refresh] no STDB connection; cross-shard mute enforcement disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 3, 5, TimeUnit.SECONDS);
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.PUNISHMENTS).thenAccept(body -> {
            try {
                apply(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[punishment-refresh] parse failed", e);
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
        long now = System.currentTimeMillis();
        boolean baselineDone = firstPollComplete;
        for (JsonElement rowElement : rows) {
            if (!rowElement.isJsonArray()) continue;
            PunishmentRow row = StdbRowParser.parse(rowElement.toString(), PunishmentRow.class);
            if (row == null) continue;
            PunishmentRecord record = StdbPersistenceGateway.toPunishmentRecord(row);
            punishments.applyRecord(record);
            boolean isActive = record.active(now);
            Boolean previously = wasActive.put(record.id(), isActive);
            if (previously == null && isActive && baselineDone && !record.pardoned()) {
                remoteEnforcer.accept(record);
            }
            if (previously != null && previously && !isActive && !record.pardoned()) {
                punishments.fireExpiry(record);
            }
        }
        firstPollComplete = true;
    }
}

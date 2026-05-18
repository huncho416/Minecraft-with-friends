package net.mythicpvp.core.transfer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.TransferRequestRow;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TransferRequestService {

    private final JavaPlugin plugin;
    private final ProxyTransferService transferService;
    private final Logger logger;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-transfer-request");
        t.setDaemon(true);
        return t;
    });

    public TransferRequestService(@NotNull JavaPlugin plugin,
                                   @NotNull ProxyTransferService transferService,
                                   @NotNull Logger logger) {
        this.plugin = plugin;
        this.transferService = transferService;
        this.logger = logger;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[transfer-request] no STDB connection; cross-shard /send and /summon disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 1, 1, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(() -> prune(connection), 60, 120, TimeUnit.SECONDS);
        logger.info("[transfer-request] polling transfer_requests every 1s");
    }

    public boolean dispatch(@NotNull UUID targetUuid, @NotNull String targetName,
                            @NotNull String destinationShard,
                            @NotNull UUID requesterUuid, @NotNull String requesterName) {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            return false;
        }
        MythicSchema schema = new MythicSchema(connection);
        schema.transferRequestCreate(targetUuid, targetName, destinationShard, requesterUuid, requesterName);
        return true;
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.TRANSFER_REQUESTS).thenAccept(body -> {
            try {
                apply(connection, body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[transfer-request] parse failed", e);
            }
        });
    }

    private void prune(@NotNull SpacetimeConnection connection) {
        MythicSchema schema = new MythicSchema(connection);
        schema.transferRequestPrune(TimeUnit.MINUTES.toMicros(5));
    }

    private void apply(@NotNull SpacetimeConnection connection, @NotNull String body) {
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
        MythicSchema schema = new MythicSchema(connection);
        for (JsonElement rowEl : rows) {
            if (!rowEl.isJsonArray()) continue;
            TransferRequestRow row = StdbRowParser.parse(rowEl.toString(), TransferRequestRow.class);
            if (row == null) continue;
            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(row.target_uuid());
            } catch (RuntimeException e) {
                continue;
            }
            long id = row.id();
            String dest = row.destination_shard();
            MythicScheduler.runSync(plugin, () -> {
                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null || !target.isOnline()) return;
                if (transferService.transfer(target, dest)) {
                    schema.transferRequestComplete(id);
                }
            });
        }
    }
}

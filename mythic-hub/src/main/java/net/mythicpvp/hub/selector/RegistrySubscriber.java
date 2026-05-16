package net.mythicpvp.hub.selector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.TableEvent;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.ServerEntryRow;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RegistrySubscriber {

    private static final Gson GSON = buildGson();

    private final JavaPlugin plugin;
    private final ServerSelectorService selectorService;
    private final Logger logger;
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
                connection.subscribeTable(TableNames.SERVER_REGISTRY, this::handleEvent);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[selector] re-subscribe failed", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void handleEvent(@NotNull TableEvent event) {
        try {
            ServerEntryRow row = GSON.fromJson(event.payload(), ServerEntryRow.class);
            if (row == null || row.shard_id() == null) {
                return;
            }
            if ("delete".equalsIgnoreCase(event.operation())) {
                MythicScheduler.runSync(plugin,
                        () -> selectorService.removeServer(row.shard_id()));
                return;
            }
            boolean healthy = "HEALTHY".equalsIgnoreCase(row.status());
            MythicScheduler.runSync(plugin, () ->
                    selectorService.updateServer(row.shard_id(), row.role(), row.player_count(), row.tps(), healthy));
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "[selector] bad registry row " + event.payload(), e);
        }
    }

    private static Gson buildGson() {
        JsonDeserializer<Long> stdbLong = (json, type, ctx) -> {
            if (json.isJsonPrimitive()) {
                JsonPrimitive p = json.getAsJsonPrimitive();
                return p.isNumber() ? p.getAsLong() : Long.parseLong(p.getAsString());
            }
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                JsonElement micros = obj.get("__timestamp_micros_since_unix_epoch__");
                if (micros == null) {
                    micros = obj.get("__time_duration_micros__");
                }
                if (micros != null && micros.isJsonPrimitive()) {
                    return micros.getAsLong();
                }
            }
            return 0L;
        };
        return new GsonBuilder()
                .registerTypeAdapter(Long.class, stdbLong)
                .registerTypeAdapter(long.class, stdbLong)
                .create();
    }
}

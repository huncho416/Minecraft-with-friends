package net.mythicpvp.core.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.TableEvent;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.ServerEntryRow;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@CommandAlias("hub|lobby")
public final class HubCommand extends MythicCommand {

    private static final String HUB_ROLE = "HUB";
    private static final String HEALTHY_STATUS = "HEALTHY";
    private static final Gson GSON = buildGson();

    private final ProxyTransferService transferService;
    private final CoreMessages messages;
    private final String localShardId;
    private final String localRole;
    private final Map<String, ServerEntryRow> hubs = new ConcurrentHashMap<>();
    private final Logger logger;

    public HubCommand(@NotNull ProxyTransferService transferService,
                      @NotNull CoreMessages messages,
                      @NotNull String localShardId,
                      @NotNull String localRole,
                      @NotNull Logger logger) {
        this.transferService = transferService;
        this.messages = messages;
        this.localShardId = localShardId;
        this.localRole = localRole;
        this.logger = logger;
        subscribeRegistry();
    }

    @Default
    public void execute(@NotNull Player player) {
        if (HUB_ROLE.equalsIgnoreCase(localRole)) {
            player.sendMessage(messages.component(
                    "messages.hub.already-here",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FFEC8AYou are already in the hub."));
            return;
        }
        ServerEntryRow target = pickHub();
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.hub.none-available",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8ANo hub servers are available right now."));
            return;
        }
        if (!transferService.transfer(player, target.shard_id())) {
            player.sendMessage(messages.component(
                    "messages.hub.transfer-failed",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8ATransfer failed."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.hub.transferring",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CTransferring to hub..."));
    }

    private void subscribeRegistry() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.warning("[hub] no STDB connection; /hub will rely on local-shard guard only");
            return;
        }
        connection.subscribeTable(TableNames.SERVER_REGISTRY, this::handleEvent);
    }

    private void handleEvent(@NotNull TableEvent event) {
        try {
            ServerEntryRow row = GSON.fromJson(event.payload(), ServerEntryRow.class);
            if (row == null || row.shard_id() == null) {
                return;
            }
            if ("delete".equalsIgnoreCase(event.operation())
                    || !HUB_ROLE.equalsIgnoreCase(row.role())
                    || !HEALTHY_STATUS.equalsIgnoreCase(row.status())) {
                hubs.remove(row.shard_id());
                return;
            }
            hubs.put(row.shard_id(), row);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "[hub] bad registry row " + event.payload(), e);
        }
    }

    private ServerEntryRow pickHub() {
        List<ServerEntryRow> candidates = new ArrayList<>(hubs.values());
        candidates.removeIf(row -> row.shard_id().equalsIgnoreCase(localShardId));
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator
                .comparingInt(ServerEntryRow::player_count)
                .thenComparing(ServerEntryRow::shard_id));
        return candidates.get(0);
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

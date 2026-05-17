package net.mythicpvp.core.staff;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.StaffChatEventRow;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StaffChatSqlRelay {

    private final StaffChannelService staffChat;
    private final String localShardId;
    private final Logger logger;
    private final AtomicLong lastSeenId = new AtomicLong(-1);
    private volatile boolean baselineDone = false;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-staff-chat-relay");
        t.setDaemon(true);
        return t;
    });

    public StaffChatSqlRelay(@NotNull StaffChannelService staffChat,
                              @NotNull String localShardId,
                              @NotNull Logger logger) {
        this.staffChat = staffChat;
        this.localShardId = localShardId;
        this.logger = logger;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[staff-chat-relay] no STDB connection; cross-shard staff chat disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 1, 1, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(() -> prune(connection), 60, 300, TimeUnit.SECONDS);
        logger.info("[staff-chat-relay] polling staff_chat_events every 1s");
    }

    public void publish(@NotNull String channel,
                        @NotNull UUID senderUuid,
                        @NotNull String senderName,
                        @NotNull String rank,
                        @NotNull String rankColor,
                        @NotNull String chatPrefix,
                        @NotNull String message) {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            return;
        }
        MythicSchema schema = new MythicSchema(connection);
        schema.staffChatSend(channel, senderUuid, senderName, rank, rankColor, chatPrefix,
                localShardId, message);
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.STAFF_CHAT_EVENTS).thenAccept(body -> {
            try {
                apply(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[staff-chat-relay] parse failed", e);
            }
        });
    }

    private void prune(@NotNull SpacetimeConnection connection) {
        MythicSchema schema = new MythicSchema(connection);
        schema.staffChatPrune(TimeUnit.MINUTES.toMicros(10));
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

        long highestThisRound = lastSeenId.get();
        for (JsonElement rowEl : rows) {
            if (!rowEl.isJsonArray()) continue;
            StaffChatEventRow row = StdbRowParser.parse(rowEl.toString(), StaffChatEventRow.class);
            if (row == null) continue;
            if (row.id() > highestThisRound) {
                highestThisRound = row.id();
            }
            if (!baselineDone) continue;
            if (row.id() <= lastSeenId.get()) continue;
            UUID senderUuid;
            try {
                senderUuid = UUID.fromString(row.sender_uuid());
            } catch (RuntimeException e) {
                continue;
            }
            staffChat.deliverRemote(
                    row.channel(),
                    row.origin_shard(),
                    senderUuid,
                    row.sender_name(),
                    row.sender_rank(),
                    row.sender_rank_color(),
                    row.sender_chat_prefix(),
                    row.message());
        }
        lastSeenId.set(highestThisRound);
        baselineDone = true;
    }
}

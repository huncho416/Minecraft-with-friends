package net.mythicpvp.core.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.core.persistence.StdbPersistenceGateway;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.FriendRequestRow;
import net.mythicpvp.suite.database.schema.dto.FriendRow;
import net.mythicpvp.suite.database.schema.dto.PartyMemberRow;
import net.mythicpvp.suite.database.schema.dto.PartyRow;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SocialSqlRefresher {

    private final SocialService social;
    private final Logger logger;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-social-refresh");
        t.setDaemon(true);
        return t;
    });

    public SocialSqlRefresher(@NotNull SocialService social, @NotNull Logger logger) {
        this.social = social;
        this.logger = logger;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[social-refresh] no STDB connection; cross-shard friends/parties disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 3, 5, TimeUnit.SECONDS);
        logger.info("[social-refresh] polling friends/parties every 5s");
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        pollTable(connection, TableNames.FRIENDS, FriendRow.class,
                row -> social.applyFriend(StdbPersistenceGateway.toFriendLink((FriendRow) row)));
        pollTable(connection, TableNames.FRIEND_REQUESTS, FriendRequestRow.class,
                row -> social.applyFriendRequest(StdbPersistenceGateway.toFriendRequest((FriendRequestRow) row)));
        pollTable(connection, TableNames.PARTIES, PartyRow.class,
                row -> social.applyParty(StdbPersistenceGateway.toParty((PartyRow) row)));
        pollTable(connection, TableNames.PARTY_MEMBERS, PartyMemberRow.class,
                row -> social.applyPartyMember(StdbPersistenceGateway.toPartyMember((PartyMemberRow) row)));
    }

    private <T> void pollTable(@NotNull SpacetimeConnection connection, @NotNull String table,
                                @NotNull Class<T> dtoType, @NotNull java.util.function.Consumer<Object> consumer) {
        connection.sql("SELECT * FROM " + table).thenAccept(body -> {
            try {
                applyRows(body, dtoType, consumer);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[social-refresh] " + table + " parse failed", e);
            }
        });
    }

    private <T> void applyRows(@NotNull String body, @NotNull Class<T> dtoType,
                                @NotNull java.util.function.Consumer<Object> consumer) {
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
            T row = StdbRowParser.parse(rowElement.toString(), dtoType);
            if (row == null) continue;
            consumer.accept(row);
        }
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
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
import java.util.logging.Logger;

@CommandAlias("hub|lobby")
public final class HubCommand extends MythicCommand {

    private static final String HUB_ROLE = "HUB";
    private static final String HEALTHY_STATUS = "HEALTHY";

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
                    "&#FFEC8AYou are already in the hub."));
            return;
        }
        ServerEntryRow target = pickHub();
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.hub.none-available",
                    "&#FF8A8ANo hub servers are available right now."));
            return;
        }
        if (!transferService.transfer(player, target.shard_id())) {
            player.sendMessage(messages.component(
                    "messages.hub.transfer-failed",
                    "&#FF8A8ATransfer failed."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.hub.transferring",
                "&#9CFF9CTransferring to hub..."));
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
        ServerEntryRow row = StdbRowParser.parse(event.payload(), ServerEntryRow.class);
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

}

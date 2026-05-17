package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.core.transfer.ShardRegistry;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.database.schema.dto.ServerEntryRow;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

@CommandAlias("hub|lobby")
public final class HubCommand extends MythicCommand {

    private static final String HUB_ROLE = "HUB";

    private final ProxyTransferService transferService;
    private final CoreMessages messages;
    private final String localShardId;
    private final String localRole;
    private final ShardRegistry shardRegistry;
    private final Logger logger;

    public HubCommand(@NotNull ProxyTransferService transferService,
                      @NotNull CoreMessages messages,
                      @NotNull String localShardId,
                      @NotNull String localRole,
                      @NotNull ShardRegistry shardRegistry,
                      @NotNull Logger logger) {
        this.transferService = transferService;
        this.messages = messages;
        this.localShardId = localShardId;
        this.localRole = localRole;
        this.shardRegistry = shardRegistry;
        this.logger = logger;
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

    private ServerEntryRow pickHub() {
        List<ServerEntryRow> candidates = shardRegistry.all().stream()
                .filter(row -> HUB_ROLE.equalsIgnoreCase(row.role()))
                .filter(row -> !row.shard_id().equalsIgnoreCase(localShardId))
                .filter(row -> !isProxyEntry(row))
                .sorted(Comparator
                        .comparingInt(ServerEntryRow::player_count)
                        .thenComparing(ServerEntryRow::shard_id))
                .toList();
        if (candidates.isEmpty()) {
            logger.fine("[hub] no candidate hubs known to ShardRegistry");
            return null;
        }
        return candidates.get(0);
    }

    private static boolean isProxyEntry(@NotNull ServerEntryRow row) {
        String id = row.shard_id();
        String addr = row.address();
        if (id != null && id.regionMatches(true, 0, "proxy", 0, 5)) {
            return true;
        }
        return addr != null && (addr.startsWith("0.0.0.0") || addr.startsWith("[::]") || addr.startsWith(":"));
    }
}

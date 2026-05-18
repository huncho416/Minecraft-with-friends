package net.mythicpvp.core.staff;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class AdminNotifyService {

    public static final String CHANNEL = "ADMIN_NOTIFY";
    public static final String PERMISSION = "mythic.core.adminnotify";

    private static final UUID SYSTEM_UUID = new UUID(0L, 0L);

    private final StaffChatSqlRelay relay;
    private final String shardId;

    public AdminNotifyService(@NotNull StaffChatSqlRelay relay, @NotNull String shardId) {
        this.relay = relay;
        this.shardId = shardId;
    }

    public void announceStartup() {
        broadcastLocal(formatOnline(shardId));
        relay.publish(CHANNEL, SYSTEM_UUID, shardId, "", "", "", "online");
    }

    public void announceShutdown() {
        broadcastLocal(formatOffline(shardId));
        relay.publish(CHANNEL, SYSTEM_UUID, shardId, "", "", "", "offline");
    }

    public static void deliverFromRemote(@NotNull String originShard, @NotNull String message) {
        String text = "offline".equalsIgnoreCase(message)
                ? formatOffline(originShard)
                : formatOnline(originShard);
        broadcastLocal(text);
    }

    @NotNull
    private static String formatOnline(@NotNull String shard) {
        return "&#7A8AA0[&#9CFF9C" + shard + "&#7A8AA0] is now &#9CFF9Conline&#7A8AA0.";
    }

    @NotNull
    private static String formatOffline(@NotNull String shard) {
        return "&#7A8AA0[&#FF8A8A" + shard + "&#7A8AA0] is now &#FF8A8Aoffline&#7A8AA0.";
    }

    private static void broadcastLocal(@NotNull String text) {
        Component message = MythicHex.colorize(text);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(PERMISSION)) {
                viewer.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }
}

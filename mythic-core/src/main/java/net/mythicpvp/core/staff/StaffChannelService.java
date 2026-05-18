package net.mythicpvp.core.staff;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StaffChannelService {

    public static final String CHANNEL = "core:staff-chat";
    private final ProtocolManager protocolManager;
    private final String serverId;
    private final List<StaffAudience> audiences = new CopyOnWriteArrayList<>();
    private final List<StaffMessage> history = new CopyOnWriteArrayList<>();
    private final Map<UUID, StaffChannel> toggles = new ConcurrentHashMap<>();
    private volatile StaffChatSqlRelay relay = null;

    public StaffChannelService(@NotNull ProtocolManager protocolManager, @NotNull String serverId) {
        this.protocolManager = protocolManager;
        this.serverId = serverId;
        this.protocolManager.subscribe(CHANNEL, message -> receive(message.deserialize(StaffMessage.class)));
    }

    public void addAudience(@NotNull StaffAudience audience) {
        audiences.add(audience);
    }

    public void send(@NotNull StaffChannel channel, @NotNull UUID senderUuid, @NotNull String senderName,
                     @NotNull String rank, @NotNull String rankColor, @NotNull String chatPrefix,
                     @NotNull String message) {
        StaffMessage staffMessage = new StaffMessage(channel, serverId, senderUuid, senderName,
                rank, rankColor, chatPrefix, message, System.currentTimeMillis());
        protocolManager.publish(CHANNEL, staffMessage);
        if (relay != null) {
            relay.publish(channel.name(), senderUuid, senderName, rank, rankColor, chatPrefix, message);
        }
    }

    public void setRelay(@NotNull StaffChatSqlRelay relay) {
        this.relay = relay;
    }

    public void deliverRemote(@NotNull String channelName,
                               @NotNull String originShard,
                               @NotNull UUID senderUuid,
                               @NotNull String senderName,
                               @NotNull String rank,
                               @NotNull String rankColor,
                               @NotNull String chatPrefix,
                               @NotNull String message) {
        if (originShard.equalsIgnoreCase(serverId)) {
            return;
        }
        if ("HELPOP_NOTIFY".equalsIgnoreCase(channelName)) {
            net.mythicpvp.core.report.StaffNotifier.notifyHelpopByName(senderName, originShard, message);
            return;
        }
        if ("REQUEST_NOTIFY".equalsIgnoreCase(channelName)) {
            net.mythicpvp.core.report.StaffNotifier.notifyHelpopByName(senderName, originShard, message);
            return;
        }
        StaffChannel channel;
        try {
            channel = StaffChannel.valueOf(channelName);
        } catch (IllegalArgumentException e) {
            return;
        }
        StaffMessage staffMessage = new StaffMessage(channel, originShard, senderUuid, senderName,
                rank, rankColor, chatPrefix, message, System.currentTimeMillis());
        receive(staffMessage);
    }

    public boolean toggle(@NotNull UUID playerUuid, @NotNull StaffChannel channel) {
        StaffChannel current = toggles.get(playerUuid);
        if (current == channel) {
            toggles.remove(playerUuid);
            return false;
        }
        toggles.put(playerUuid, channel);
        return true;
    }

    public void clearToggle(@NotNull UUID playerUuid) {
        toggles.remove(playerUuid);
    }

    public StaffChannel toggledChannel(@NotNull UUID playerUuid) {
        return toggles.get(playerUuid);
    }

    @NotNull
    public List<StaffMessage> history() {
        return List.copyOf(history);
    }

    private void receive(@NotNull StaffMessage message) {
        history.add(message);
        for (StaffAudience audience : audiences) {
            audience.accept(message);
        }
    }
}

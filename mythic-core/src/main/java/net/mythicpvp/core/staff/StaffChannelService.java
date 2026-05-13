package net.mythicpvp.core.staff;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StaffChannelService {

    public static final String CHANNEL = "core:staff-chat";
    private final ProtocolManager protocolManager;
    private final String serverId;
    private final List<StaffAudience> audiences = new CopyOnWriteArrayList<>();
    private final List<StaffMessage> history = new CopyOnWriteArrayList<>();

    public StaffChannelService(@NotNull ProtocolManager protocolManager, @NotNull String serverId) {
        this.protocolManager = protocolManager;
        this.serverId = serverId;
        this.protocolManager.subscribe(CHANNEL, message -> receive(message.deserialize(StaffMessage.class)));
    }

    public void addAudience(@NotNull StaffAudience audience) {
        audiences.add(audience);
    }

    public void send(@NotNull StaffChannel channel, @NotNull UUID senderUuid, @NotNull String senderName, @NotNull String rank, @NotNull String rankColor, @NotNull String message) {
        StaffMessage staffMessage = new StaffMessage(channel, serverId, senderUuid, senderName, rank, rankColor, message, System.currentTimeMillis());
        protocolManager.publish(CHANNEL, staffMessage);
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

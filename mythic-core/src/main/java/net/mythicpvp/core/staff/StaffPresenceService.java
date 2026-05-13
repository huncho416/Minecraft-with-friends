package net.mythicpvp.core.staff;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class StaffPresenceService {

    public static final String CHANNEL = "core:staff-presence";
    private final ProtocolManager protocolManager;
    private final String serverId;
    private final List<Consumer<StaffPresenceEvent>> audiences = new CopyOnWriteArrayList<>();
    private final List<StaffPresenceEvent> history = new CopyOnWriteArrayList<>();

    public StaffPresenceService(@NotNull ProtocolManager protocolManager, @NotNull String serverId) {
        this.protocolManager = protocolManager;
        this.serverId = serverId;
        this.protocolManager.subscribe(CHANNEL, message -> receive(message.deserialize(StaffPresenceEvent.class)));
    }

    public void addAudience(@NotNull Consumer<StaffPresenceEvent> audience) {
        audiences.add(audience);
    }

    public void join(@NotNull UUID staffUuid, @NotNull String staffName, @NotNull String rank, @NotNull String rankColor) {
        publish(new StaffPresenceEvent(StaffPresenceType.JOIN, serverId, staffUuid, staffName, rank, rankColor, "", serverId, System.currentTimeMillis()));
    }

    public void quit(@NotNull UUID staffUuid, @NotNull String staffName, @NotNull String rank, @NotNull String rankColor) {
        publish(new StaffPresenceEvent(StaffPresenceType.QUIT, serverId, staffUuid, staffName, rank, rankColor, serverId, "", System.currentTimeMillis()));
    }

    public void switchServer(@NotNull UUID staffUuid, @NotNull String staffName, @NotNull String rank, @NotNull String rankColor, @NotNull String fromServer, @NotNull String toServer) {
        publish(new StaffPresenceEvent(StaffPresenceType.SWITCH, serverId, staffUuid, staffName, rank, rankColor, fromServer, toServer, System.currentTimeMillis()));
    }

    @NotNull
    public List<StaffPresenceEvent> history() {
        return List.copyOf(history);
    }

    private void publish(@NotNull StaffPresenceEvent event) {
        protocolManager.publish(CHANNEL, event);
    }

    private void receive(@NotNull StaffPresenceEvent event) {
        history.add(event);
        for (Consumer<StaffPresenceEvent> audience : audiences) {
            audience.accept(event);
        }
    }
}

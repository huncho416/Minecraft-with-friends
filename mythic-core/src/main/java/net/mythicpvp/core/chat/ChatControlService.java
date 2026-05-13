package net.mythicpvp.core.chat;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ChatControlService {

    public static final String CHANNEL = "core:chat-control";
    private final ProtocolManager protocolManager;
    private final List<ChatControlState> history = new CopyOnWriteArrayList<>();
    private volatile ChatControlState state = new ChatControlState(false, 0, ChatScope.LOCAL);

    public ChatControlService(@NotNull ProtocolManager protocolManager) {
        this.protocolManager = protocolManager;
        this.protocolManager.subscribe(CHANNEL, message -> apply(message.deserialize(ChatControlState.class)));
    }

    public void mute(@NotNull ChatScope scope) {
        publish(new ChatControlState(true, state.slowSeconds(), scope));
    }

    public void unmute(@NotNull ChatScope scope) {
        publish(new ChatControlState(false, state.slowSeconds(), scope));
    }

    public void slow(int seconds, @NotNull ChatScope scope) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds");
        }
        publish(new ChatControlState(state.muted(), seconds, scope));
    }

    public void clear(@NotNull ChatScope scope) {
        publish(new ChatControlState(state.muted(), state.slowSeconds(), scope));
    }

    @NotNull
    public ChatControlState state() {
        return state;
    }

    @NotNull
    public List<ChatControlState> history() {
        return List.copyOf(history);
    }

    private void publish(@NotNull ChatControlState next) {
        protocolManager.publish(CHANNEL, next);
    }

    private void apply(@NotNull ChatControlState next) {
        state = next;
        history.add(next);
    }
}

package net.mythicpvp.core.chat;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class ChatControlService {

    public static final String CHANNEL = "core:chat-control";

    public interface ClearListener {
        void onClear();
    }

    private final ProtocolManager protocolManager;
    private final String shardId;
    private final List<ChatControlState> history = new CopyOnWriteArrayList<>();
    private final List<ClearListener> clearListeners = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> lastMessageMillis = new ConcurrentHashMap<>();
    private final AtomicLong clearTick = new AtomicLong();
    private volatile ChatControlState state = new ChatControlState(false, 0, ChatScope.LOCAL);

    @Deprecated
    public ChatControlService(@NotNull ProtocolManager protocolManager) {
        this(protocolManager, "");
    }

    public ChatControlService(@NotNull ProtocolManager protocolManager, @NotNull String shardId) {
        this.protocolManager = protocolManager;
        this.shardId = shardId;
        this.protocolManager.subscribe(CHANNEL,
                message -> apply(message.deserialize(ChatControlState.class)));
    }

    public void mute(@NotNull ChatScope scope) {
        publish(new ChatControlState(true, state.slowSeconds(), scope, shardId, state.clearTick()));
    }

    public void unmute(@NotNull ChatScope scope) {
        publish(new ChatControlState(false, state.slowSeconds(), scope, shardId, state.clearTick()));
    }

    public void slow(int seconds, @NotNull ChatScope scope) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds");
        }
        publish(new ChatControlState(state.muted(), seconds, scope, shardId, state.clearTick()));
    }

    public void clear(@NotNull ChatScope scope) {
        long next = clearTick.incrementAndGet();
        publish(new ChatControlState(state.muted(), state.slowSeconds(), scope, shardId, next));
    }

    public long registerMessage(@NotNull UUID player, long nowMillis) {
        int slowSeconds = state.slowSeconds();
        if (slowSeconds <= 0) {

            lastMessageMillis.put(player, nowMillis);
            return 0L;
        }
        Long previous = lastMessageMillis.get(player);
        long cooldownMillis = (long) slowSeconds * 1000L;
        if (previous != null && nowMillis - previous < cooldownMillis) {
            return cooldownMillis - (nowMillis - previous);
        }
        lastMessageMillis.put(player, nowMillis);
        return 0L;
    }

    public void forget(@NotNull UUID player) {
        lastMessageMillis.remove(player);
    }

    public boolean muted() {
        return state.muted();
    }

    public int slowSeconds() {
        return state.slowSeconds();
    }

    @NotNull
    public String shardId() {
        return shardId;
    }

    public void onClear(@NotNull ClearListener listener) {
        clearListeners.add(listener);
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

        if (next.scope() == ChatScope.LOCAL
                && !shardId.isEmpty()
                && !next.originServer().isEmpty()
                && !next.originServer().equals(shardId)) {
            return;
        }
        boolean clearedThisTick = next.clearTick() > state.clearTick();
        state = next;
        history.add(next);
        if (clearedThisTick) {
            for (ClearListener listener : clearListeners) {
                try {
                    listener.onClear();
                } catch (RuntimeException ignored) {

                }
            }
        }
    }
}

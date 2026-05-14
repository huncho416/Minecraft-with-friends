package net.mythicpvp.core.chat;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the network-replicated chat-control state — global mute, slow
 * mode, and "clear chat" pulses — plus per-player slow-mode bookkeeping.
 *
 * <p>State changes flow through the {@code core:chat-control} protocol
 * channel so every server hears them. Scope semantics:
 *
 * <ul>
 *   <li>{@link ChatScope#NETWORK} — every receiver applies the change.
 *   <li>{@link ChatScope#LOCAL} — only the originating server applies
 *       it. Receivers compare {@link ChatControlState#originServer()}
 *       against their own shard id and drop foreign LOCAL state.
 * </ul>
 *
 * <p>The legacy {@link #ChatControlService(ProtocolManager)} constructor
 * keeps existing tests green by treating the service as the only one in
 * its JVM (originServer="", scope checks succeed for all messages).
 */
public final class ChatControlService {

    public static final String CHANNEL = "core:chat-control";

    /** Listeners that want to react to a "clear chat" pulse hitting the server. */
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

    /**
     * @deprecated Use {@link #ChatControlService(ProtocolManager, String)}
     *             so {@code LOCAL} scope can be enforced. Retained because
     *             the existing {@code ChatControlServiceTest} constructs
     *             two instances against one protocol manager and depends
     *             on the empty-shardId match-all behaviour.
     */
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

    // ── Mutators ─────────────────────────────────────────────────────

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

    /**
     * Emit a "clear chat" pulse. The listener tier (e.g. {@code ChatGuard})
     * subscribes via {@link #onClear} to flood blank lines into every
     * online player's chat. State (mute/slow) is preserved; only the
     * {@code clearTick} bumps so subscribers can tell this is a discrete
     * event rather than a same-state republish.
     */
    public void clear(@NotNull ChatScope scope) {
        long next = clearTick.incrementAndGet();
        publish(new ChatControlState(state.muted(), state.slowSeconds(), scope, shardId, next));
    }

    // ── Per-player slow-mode bookkeeping ────────────────────────────

    /**
     * Record that {@code player} just sent a message. Returns the number
     * of milliseconds the player must wait before sending again, or 0 if
     * they may send immediately (slow mode disabled or cool-down met).
     *
     * <p>Always records — the cool-down resets from each call so a
     * burst of attempts can't game the limit. Caller is expected to
     * cancel the chat event when this returns &gt; 0.
     */
    public long registerMessage(@NotNull UUID player, long nowMillis) {
        int slowSeconds = state.slowSeconds();
        if (slowSeconds <= 0) {
            // Track timestamp anyway so a slow-mode change later doesn't
            // give an immediate free-pass to the heaviest chatters.
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

    /** Drop slow-mode bookkeeping for a player (e.g. on quit). */
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

    // ── Listeners ────────────────────────────────────────────────────

    /**
     * Register a callback for "clear chat" pulses. The listener is run
     * on whatever thread the protocol callback fires on — implementations
     * that touch Bukkit state should reschedule onto main themselves.
     */
    public void onClear(@NotNull ClearListener listener) {
        clearListeners.add(listener);
    }

    // ── Read API ─────────────────────────────────────────────────────

    @NotNull
    public ChatControlState state() {
        return state;
    }

    @NotNull
    public List<ChatControlState> history() {
        return List.copyOf(history);
    }

    // ── Internals ────────────────────────────────────────────────────

    private void publish(@NotNull ChatControlState next) {
        protocolManager.publish(CHANNEL, next);
    }

    private void apply(@NotNull ChatControlState next) {
        // LOCAL state from a different shard isn't ours — drop it. An
        // empty shardId / empty origin matches everything (legacy tests).
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
                    // Listener errors must not cascade to other listeners
                    // or back into the protocol callback.
                }
            }
        }
    }
}

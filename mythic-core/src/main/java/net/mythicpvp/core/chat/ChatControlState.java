package net.mythicpvp.core.chat;

import org.jetbrains.annotations.NotNull;

/**
 * Snapshot of network-wide chat-control state.
 *
 * <p>Distributed across servers via {@link ChatControlService} on the
 * {@code core:chat-control} protocol channel.
 *
 * @param muted        whether chat is currently muted
 * @param slowSeconds  per-player cool-down between messages, 0 = off
 * @param scope        {@link ChatScope#LOCAL} = applies only on the
 *                     server identified by {@code originServer};
 *                     {@link ChatScope#NETWORK} = applies everywhere
 * @param originServer shard id that emitted the state — receivers ignore
 *                     {@code LOCAL} messages from a different shard.
 *                     Empty string means "unknown / single-server"
 *                     (matches the legacy zero-arg constructor for
 *                     backward-compat tests).
 * @param clearTick    monotonic counter bumped by {@link
 *                     ChatControlService#clear(ChatScope)}; receivers
 *                     compare against the previous value to know a
 *                     "wipe chat" pulse fired without conflating it
 *                     with a same-state republish.
 */
public record ChatControlState(
        boolean muted,
        int slowSeconds,
        @NotNull ChatScope scope,
        @NotNull String originServer,
        long clearTick
) {

    /** Backward-compat constructor used by the legacy tests. */
    public ChatControlState(boolean muted, int slowSeconds, @NotNull ChatScope scope) {
        this(muted, slowSeconds, scope, "", 0L);
    }
}

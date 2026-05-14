package net.mythicpvp.core.announce;

import org.jetbrains.annotations.NotNull;

/**
 * Wire payload for the {@code core:broadcast} protocol channel. One
 * line of pre-formatted Mythic-hex text broadcast to every player on
 * every server.
 *
 * @param message  pre-rendered chat line (already passed through the
 *                 broadcast format template; receivers don't re-format)
 * @param origin   shard id that issued the broadcast — receivers use
 *                 this to skip echoing it back to the originating shard
 */
public record BroadcastNotice(@NotNull String message, @NotNull String origin) {}

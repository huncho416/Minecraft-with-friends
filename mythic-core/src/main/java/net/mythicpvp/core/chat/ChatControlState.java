package net.mythicpvp.core.chat;

import org.jetbrains.annotations.NotNull;

public record ChatControlState(
        boolean muted,
        int slowSeconds,
        @NotNull ChatScope scope,
        @NotNull String originServer,
        long clearTick
) {

    public ChatControlState(boolean muted, int slowSeconds, @NotNull ChatScope scope) {
        this(muted, slowSeconds, scope, "", 0L);
    }
}

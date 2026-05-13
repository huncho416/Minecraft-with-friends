package net.mythicpvp.suite.database;

import org.jetbrains.annotations.NotNull;

public interface SpacetimeCodec {
    byte @NotNull [] encode(@NotNull Object value);

    @NotNull
    <T> T decode(byte @NotNull [] bytes, @NotNull Class<T> type);
}

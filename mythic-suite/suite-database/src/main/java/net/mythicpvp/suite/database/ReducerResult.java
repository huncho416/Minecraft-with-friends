package net.mythicpvp.suite.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ReducerResult(@NotNull String requestId, boolean success, @Nullable String payload, @Nullable String error) {}

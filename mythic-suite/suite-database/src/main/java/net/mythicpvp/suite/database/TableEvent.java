package net.mythicpvp.suite.database;

import org.jetbrains.annotations.NotNull;

public record TableEvent(@NotNull String table, @NotNull String payload, @NotNull String operation) {}

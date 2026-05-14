package net.mythicpvp.core.announce;

import org.jetbrains.annotations.NotNull;

public record BroadcastNotice(@NotNull String message, @NotNull String origin) {}

package net.mythicpvp.core.chat;

import org.jetbrains.annotations.NotNull;

public record ChatControlState(boolean muted, int slowSeconds, @NotNull ChatScope scope) {}

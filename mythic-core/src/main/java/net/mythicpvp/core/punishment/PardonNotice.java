package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

public record PardonNotice(@NotNull PunishmentRecord record, @NotNull String staffName, boolean silent) {}

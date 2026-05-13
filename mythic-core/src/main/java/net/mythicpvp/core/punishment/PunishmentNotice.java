package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

public record PunishmentNotice(@NotNull PunishmentRecord record, boolean publicBroadcast) {}

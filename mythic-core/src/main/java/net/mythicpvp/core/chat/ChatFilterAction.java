package net.mythicpvp.core.chat;

import net.mythicpvp.core.punishment.PunishmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public record ChatFilterAction(
        @NotNull Kind kind,
        @Nullable PunishmentType punishmentType,
        @Nullable Duration duration,
        @NotNull String message
) {
    public enum Kind {
        WARN_ONLY,
        WARN_HISTORY,
        MUTE,
        FINAL_WARNING
    }

    public static ChatFilterAction warnOnly(@NotNull String message) {
        return new ChatFilterAction(Kind.WARN_ONLY, null, null, message);
    }

    public static ChatFilterAction warnHistory(@NotNull String message) {
        return new ChatFilterAction(Kind.WARN_HISTORY, PunishmentType.WARN, null, message);
    }

    public static ChatFilterAction mute(@NotNull Duration duration, @NotNull String message) {
        PunishmentType type = duration == null ? PunishmentType.MUTE : PunishmentType.TEMP_MUTE;
        return new ChatFilterAction(Kind.MUTE, type, duration, message);
    }

    public static ChatFilterAction permanentMute(@NotNull String message) {
        return new ChatFilterAction(Kind.MUTE, PunishmentType.MUTE, null, message);
    }

    public static ChatFilterAction finalWarning(@NotNull String message) {
        return new ChatFilterAction(Kind.FINAL_WARNING, null, null, message);
    }
}

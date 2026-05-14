package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mirrors {@code common::punishment_kind}. Schema v2: {@code BAN} replaces
 * the v1 {@code PERMA_BAN} value, and {@code TEMP_MUTE}/{@code BLACKLIST}
 * are added so the DB matches mythic-core's {@code PunishmentType} enum.
 */
public enum PunishmentKind {
    WARN("WARN"),
    MUTE("MUTE"),
    TEMP_MUTE("TEMP_MUTE"),
    KICK("KICK"),
    BAN("BAN"),
    TEMP_BAN("TEMP_BAN"),
    BLACKLIST("BLACKLIST");

    private final String wire;

    PunishmentKind(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static PunishmentKind fromWire(@NotNull String wire) {
        for (PunishmentKind k : values()) {
            if (k.wire.equals(wire)) {
                return k;
            }
        }
        return null;
    }
}

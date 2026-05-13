package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Mirrors {@code common::punishment_kind}. */
public enum PunishmentKind {
    WARN("WARN"),
    MUTE("MUTE"),
    KICK("KICK"),
    TEMP_BAN("TEMP_BAN"),
    PERMA_BAN("PERMA_BAN");

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

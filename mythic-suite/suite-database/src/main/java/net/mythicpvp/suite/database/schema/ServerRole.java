package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Mirrors {@code common::server_role}. */
public enum ServerRole {
    HUB("HUB"),
    SKYBLOCK("SKYBLOCK"),
    PVP("PVP"),
    EVENT("EVENT");

    private final String wire;

    ServerRole(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static ServerRole fromWire(@NotNull String wire) {
        for (ServerRole r : values()) {
            if (r.wire.equals(wire)) {
                return r;
            }
        }
        return null;
    }
}

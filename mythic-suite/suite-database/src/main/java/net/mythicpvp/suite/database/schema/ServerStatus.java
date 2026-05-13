package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Mirrors {@code common::server_status}. */
public enum ServerStatus {
    STARTING("STARTING"),
    HEALTHY("HEALTHY"),
    DEGRADED("DEGRADED"),
    DRAINING("DRAINING"),
    OFFLINE("OFFLINE");

    private final String wire;

    ServerStatus(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static ServerStatus fromWire(@NotNull String wire) {
        for (ServerStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        return null;
    }
}

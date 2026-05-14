package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum StdbCurrency {
    COINS("COINS"),
    POINTS("POINTS"),
    GEMS("GEMS");

    private final String wire;

    StdbCurrency(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static StdbCurrency fromWire(@NotNull String wire) {
        for (StdbCurrency c : values()) {
            if (c.wire.equals(wire)) {
                return c;
            }
        }
        return null;
    }
}

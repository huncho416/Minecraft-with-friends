package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mirrors {@code common::currency} in {@code mythic-cord/stdb}.
 *
 * <p>Distinct from {@code suite-api}'s {@code Currency} so the API module
 * stays free of database concerns; the two are kept in sync by hand.
 */
public enum StdbCurrency {
    COINS("COINS"),
    POINTS("POINTS"),
    GEMS("GEMS");

    private final String wire;

    StdbCurrency(@NotNull String wire) {
        this.wire = wire;
    }

    /** Exact string written to STDB. */
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

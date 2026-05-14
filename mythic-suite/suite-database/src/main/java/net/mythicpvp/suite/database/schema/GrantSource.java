package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mirrors {@code common::grant_source}. Identifies why a rank grant exists
 * — staff promotion, store purchase, automated promotion, or system seed.
 */
public enum GrantSource {
    STAFF("STAFF"),
    PURCHASE("PURCHASE"),
    PROMOTION("PROMOTION"),
    SYSTEM("SYSTEM");

    private final String wire;

    GrantSource(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static GrantSource fromWire(@NotNull String wire) {
        for (GrantSource s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        return null;
    }
}

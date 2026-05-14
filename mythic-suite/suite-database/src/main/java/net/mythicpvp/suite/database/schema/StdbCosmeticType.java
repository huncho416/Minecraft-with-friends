package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum StdbCosmeticType {
    HAT("HAT"),
    TITLE("TITLE"),
    PARTICLE("PARTICLE"),
    KILL_EFFECT("KILL_EFFECT"),
    WIN_EFFECT("WIN_EFFECT"),
    CHAT_TAG("CHAT_TAG");

    private final String wire;

    StdbCosmeticType(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static StdbCosmeticType fromWire(@NotNull String wire) {
        for (StdbCosmeticType t : values()) {
            if (t.wire.equals(wire)) {
                return t;
            }
        }
        return null;
    }
}

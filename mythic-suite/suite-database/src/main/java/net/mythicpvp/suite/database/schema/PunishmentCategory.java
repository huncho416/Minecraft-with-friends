package net.mythicpvp.suite.database.schema;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mirrors {@code common::punishment_category}. Used for punishment
 * template grouping (the four wool colors in the {@code /punish} menu).
 *
 * <p>Distinct from {@code mythic-core}'s gameplay-side
 * {@code PunishmentCategory} enum so the DB layer doesn't import
 * Bukkit {@code Material}.
 */
public enum PunishmentCategory {
    WARN("WARN"),
    MUTE("MUTE"),
    BAN("BAN"),
    BLACKLIST("BLACKLIST");

    private final String wire;

    PunishmentCategory(@NotNull String wire) {
        this.wire = wire;
    }

    @NotNull
    public String wireValue() {
        return wire;
    }

    @Nullable
    public static PunishmentCategory fromWire(@NotNull String wire) {
        for (PunishmentCategory c : values()) {
            if (c.wire.equals(wire)) {
                return c;
            }
        }
        return null;
    }
}

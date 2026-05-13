package net.mythicpvp.suite.api;

import org.jetbrains.annotations.NotNull;

public enum Currency {
    COINS("Coins", "&#FF00F8"),
    POINTS("Points", "&#FFFFFF"),
    GEMS("Gems", "&#FF9FFC");

    private final String displayName;
    private final String hexColor;

    Currency(@NotNull String displayName, @NotNull String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public String getHexColor() {
        return hexColor;
    }
}

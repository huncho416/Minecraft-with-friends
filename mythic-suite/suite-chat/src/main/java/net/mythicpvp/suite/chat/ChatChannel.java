package net.mythicpvp.suite.chat;

import org.jetbrains.annotations.NotNull;

public enum ChatChannel {
    GLOBAL("Global", "&#FFFFFF"),
    STAFF("Staff", "&#FF00F8"),
    ISLAND("Island", "&#FF9FFC"),
    PVP("PvP Zone", "&#FF4040"),
    PARTY("Party", "&#40FF40");

    private final String displayName;
    private final String hexColor;

    ChatChannel(@NotNull String displayName, @NotNull String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    @NotNull public String getDisplayName() { return displayName; }
    @NotNull public String getHexColor() { return hexColor; }
}

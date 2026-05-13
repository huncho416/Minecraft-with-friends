package net.mythicpvp.suite.cosmetic;

import org.jetbrains.annotations.NotNull;

public enum CosmeticType {
    HAT("Hats"),
    TITLE("Titles"),
    PARTICLE("Particles"),
    KILL_EFFECT("Kill Effects"),
    WIN_EFFECT("Win Effects"),
    CHAT_TAG("Chat Tags");

    private final String displayName;

    CosmeticType(@NotNull String displayName) {
        this.displayName = displayName;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }
}

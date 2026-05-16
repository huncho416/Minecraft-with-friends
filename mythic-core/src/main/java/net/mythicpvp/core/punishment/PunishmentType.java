package net.mythicpvp.core.punishment;

import org.jetbrains.annotations.NotNull;

public enum PunishmentType {
    BAN("ban", true, false),
    TEMP_BAN("tempban", true, true),
    MUTE("mute", false, false),
    TEMP_MUTE("tempmute", false, true),
    BLACKLIST("blacklist", true, false),
    WARN("warn", false, false),
    KICK("kick", false, false);

    private final String command;
    private final boolean loginBlocking;
    private final boolean temporary;

    PunishmentType(@NotNull String command, boolean loginBlocking, boolean temporary) {
        this.command = command;
        this.loginBlocking = loginBlocking;
        this.temporary = temporary;
    }

    @NotNull
    public String command() {
        return command;
    }

    public boolean loginBlocking() {
        return loginBlocking;
    }

    public boolean temporary() {
        return temporary;
    }
}

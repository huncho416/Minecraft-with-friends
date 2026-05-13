package net.mythicpvp.core.staff;

import org.jetbrains.annotations.NotNull;

public enum StaffChannel {
    STAFF("staff", "mythic.core.staffchat"),
    BUILDER("builder", "mythic.core.builderchat"),
    MANAGEMENT("management", "mythic.core.managementchat"),
    ADMIN("admin", "mythic.core.adminchat"),
    OWNER("owner", "mythic.core.ownerchat");

    private final String id;
    private final String permission;

    StaffChannel(@NotNull String id, @NotNull String permission) {
        this.id = id;
        this.permission = permission;
    }

    @NotNull
    public String id() {
        return id;
    }

    @NotNull
    public String permission() {
        return permission;
    }
}

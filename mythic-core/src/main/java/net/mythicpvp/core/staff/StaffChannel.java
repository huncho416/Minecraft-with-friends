package net.mythicpvp.core.staff;

import org.jetbrains.annotations.NotNull;

public enum StaffChannel {
    STAFF("staff", "mythic.core.staffchat", "SC", "&#9CFF9C"),
    BUILDER("builder", "mythic.core.builderchat", "BC", "&#FFEC8A"),
    MANAGEMENT("management", "mythic.core.managementchat", "MC", "&#8AC7FF"),
    ADMIN("admin", "mythic.core.adminchat", "AC", "&#FF8A8A"),
    OWNER("owner", "mythic.core.ownerchat", "OC", "&#F529BE");

    private final String id;
    private final String permission;
    private final String tag;
    private final String tagColor;

    StaffChannel(@NotNull String id, @NotNull String permission, @NotNull String tag, @NotNull String tagColor) {
        this.id = id;
        this.permission = permission;
        this.tag = tag;
        this.tagColor = tagColor;
    }

    @NotNull
    public String id() {
        return id;
    }

    @NotNull
    public String permission() {
        return permission;
    }

    @NotNull
    public String tag() {
        return tag;
    }

    @NotNull
    public String tagColor() {
        return tagColor;
    }
}

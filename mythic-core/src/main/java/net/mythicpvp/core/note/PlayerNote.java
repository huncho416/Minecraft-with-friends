package net.mythicpvp.core.note;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerNote {

    private final long id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID authorUuid;
    private final String authorName;
    private final String title;
    private final String body;
    private final String serverId;
    private final long createdAt;
    private volatile boolean active = true;

    public PlayerNote(long id,
                      @NotNull UUID targetUuid,
                      @NotNull String targetName,
                      @NotNull UUID authorUuid,
                      @NotNull String authorName,
                      @NotNull String title,
                      @NotNull String body,
                      @NotNull String serverId,
                      long createdAt) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.title = title;
        this.body = body;
        this.serverId = serverId;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    @NotNull
    public UUID targetUuid() {
        return targetUuid;
    }

    @NotNull
    public String targetName() {
        return targetName;
    }

    @NotNull
    public UUID authorUuid() {
        return authorUuid;
    }

    @NotNull
    public String authorName() {
        return authorName;
    }

    @NotNull
    public String title() {
        return title;
    }

    @NotNull
    public String body() {
        return body;
    }

    @NotNull
    public String serverId() {
        return serverId;
    }

    public long createdAt() {
        return createdAt;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

package net.mythicpvp.suite.event;

import org.jetbrains.annotations.NotNull;

public abstract class MythicEvent {

    private boolean cancelled = false;
    private final String name;

    protected MythicEvent() {
        this.name = getClass().getSimpleName();
    }

    protected MythicEvent(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getEventName() {
        return name;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

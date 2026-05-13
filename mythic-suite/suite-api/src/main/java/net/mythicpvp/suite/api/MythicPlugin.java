package net.mythicpvp.suite.api;

import org.jetbrains.annotations.NotNull;

public interface MythicPlugin {

    void enable();

    void disable();

    void reload();

    @NotNull String getServerIdentifier();
}

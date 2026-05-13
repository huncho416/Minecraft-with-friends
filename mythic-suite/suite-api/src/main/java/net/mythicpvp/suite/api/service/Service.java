package net.mythicpvp.suite.api.service;

import org.jetbrains.annotations.NotNull;

public interface Service {

    @NotNull String getName();

    void initialize();

    void shutdown();
}

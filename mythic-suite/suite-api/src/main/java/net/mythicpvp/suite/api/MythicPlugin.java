package net.mythicpvp.suite.api;

import net.mythicpvp.suite.api.error.MythicErrorTracker;
import org.jetbrains.annotations.NotNull;

public interface MythicPlugin {

    void enable();

    void disable();

    void reload();

    @NotNull String getServerIdentifier();

    default void enableTracked() {
        MythicErrorTracker.initFromEnvironment(getServerIdentifier());
        try {
            enable();
        } catch (RuntimeException e) {
            MythicErrorTracker.capture(e);
            throw e;
        }
    }

    default void disableTracked() {
        try {
            disable();
        } catch (RuntimeException e) {
            MythicErrorTracker.capture(e);
            throw e;
        } finally {
            MythicErrorTracker.close();
        }
    }

    default void reloadTracked() {
        try {
            reload();
        } catch (RuntimeException e) {
            MythicErrorTracker.capture(e);
            throw e;
        }
    }
}

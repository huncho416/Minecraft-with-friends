package net.mythicpvp.suite.api;

import net.mythicpvp.suite.api.error.MythicErrorTracker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MythicPluginTest {

    @AfterEach
    void tearDown() {
        MythicErrorTracker.close();
    }

    @Test
    void trackedLifecycleCallsPluginMethods() {
        TrackingPlugin plugin = new TrackingPlugin(false);
        plugin.enableTracked();
        plugin.reloadTracked();
        plugin.disableTracked();
        assertEquals(1, plugin.enabled);
        assertEquals(1, plugin.reloaded);
        assertEquals(1, plugin.disabled);
        assertFalse(MythicErrorTracker.isInitialized());
    }

    @Test
    void trackedLifecycleRethrowsPluginFailures() {
        TrackingPlugin plugin = new TrackingPlugin(true);
        assertThrows(IllegalStateException.class, plugin::enableTracked);
        assertFalse(MythicErrorTracker.isInitialized());
    }

    private static final class TrackingPlugin implements MythicPlugin {

        private final boolean fail;
        private int enabled;
        private int disabled;
        private int reloaded;

        private TrackingPlugin(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void enable() {
            enabled++;
            if (fail) {
                throw new IllegalStateException("enable failed");
            }
        }

        @Override
        public void disable() {
            disabled++;
        }

        @Override
        public void reload() {
            reloaded++;
        }

        @Override
        public @NotNull String getServerIdentifier() {
            return "test-server";
        }
    }
}

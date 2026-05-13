package net.mythicpvp.suite.api.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MythicErrorTrackerTest {

    @AfterEach
    void tearDown() {
        MythicErrorTracker.close();
    }

    @Test
    void blankDsnLeavesTrackerDisabled() {
        assertFalse(MythicErrorTracker.init("", "test", "release", "hub-1"));
        assertFalse(MythicErrorTracker.isInitialized());
    }

    @Test
    void captureDoesNothingWhenDisabled() {
        MythicErrorTracker.capture(new IllegalStateException("test"));
        MythicErrorTracker.capture("test");
        assertFalse(MythicErrorTracker.isInitialized());
    }
}

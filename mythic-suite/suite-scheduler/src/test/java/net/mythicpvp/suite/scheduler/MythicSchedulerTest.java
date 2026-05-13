package net.mythicpvp.suite.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MythicSchedulerTest {

    @Test
    void detectsRuntimeWithoutThrowing() {
        assertDoesNotThrow(MythicScheduler::isFolia);
    }
}

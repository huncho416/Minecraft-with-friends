package net.mythicpvp.core.rank;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class GrantDurationTest {

    @Test
    void parsesPermanentAliases() {
        GrantDuration duration = GrantDuration.parse("perm");
        assertTrue(duration.permanent());
        assertEquals(0L, duration.expiresAt(100L));
    }

    @Test
    void parsesDayDurations() {
        GrantDuration duration = GrantDuration.parse("7d");
        assertFalse(duration.permanent());
        assertEquals(Duration.ofDays(7), duration.duration());
    }
}

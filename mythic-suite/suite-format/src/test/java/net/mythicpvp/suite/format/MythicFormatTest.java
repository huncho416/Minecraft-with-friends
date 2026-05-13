package net.mythicpvp.suite.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MythicFormatTest {

    @Test
    void formatsNumbersDurationsAndPercent() {
        assertEquals("1.5M", MythicFormat.number(1_500_000));
        assertEquals("$1.5M", MythicFormat.money(1_500_000));
        assertEquals("1d 1h 1m 1s", MythicFormat.duration(90_061_000));
        assertEquals("75%", MythicFormat.percent(0.75));
    }

    @Test
    void parsesTimeUnits() {
        assertEquals(1_800_000, MythicFormat.parseTime("30m"));
        assertEquals(172_800_000, MythicFormat.parseTime("2d"));
        assertThrows(IllegalArgumentException.class, () -> MythicFormat.parseTime("15x"));
    }
}

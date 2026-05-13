package net.mythicpvp.core.punishment;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PunishmentCommandTest {

    @Test
    void parsesSilentBanFlagBeforeTarget() {
        PunishmentCommand command = PunishmentCommand.parse(new String[]{"-s", "Notch", "Cheating"}, false, "No reason specified");
        assertEquals("Notch", command.targetName());
        assertEquals("Cheating", command.reason());
        assertTrue(command.silent());
        assertNull(command.duration());
    }

    @Test
    void parsesSilentBanFlagAfterTarget() {
        PunishmentCommand command = PunishmentCommand.parse(new String[]{"Notch", "-s", "Cheating"}, false, "No reason specified");
        assertEquals("Notch", command.targetName());
        assertEquals("Cheating", command.reason());
        assertTrue(command.silent());
    }

    @Test
    void parsesTemporaryPunishmentDuration() {
        PunishmentCommand command = PunishmentCommand.parse(new String[]{"Notch", "2h", "-s", "Spam"}, true, "No reason specified");
        assertEquals(Duration.ofHours(2), command.duration());
        assertEquals(Instant.parse("2026-05-13T02:00:00Z"), command.expiresAt(Instant.parse("2026-05-13T00:00:00Z")));
        assertEquals("Spam", command.reason());
        assertTrue(command.silent());
    }
}

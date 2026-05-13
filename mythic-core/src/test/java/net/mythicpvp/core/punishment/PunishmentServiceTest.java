package net.mythicpvp.core.punishment;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PunishmentServiceTest {

    private final ProtocolManager protocolManager = ProtocolManager.getInstance();

    @AfterEach
    void tearDown() {
        protocolManager.clear();
    }

    @Test
    void silentPunishmentDoesNotCreatePublicBroadcast() {
        PunishmentService service = new PunishmentService(protocolManager, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();
        PunishmentRecord record = service.punish(new PunishmentRequest(target, "Target", UUID.randomUUID(), "Staff", PunishmentType.BAN, "Cheating", null, true, "skyblock"));
        assertTrue(record.silent());
        assertEquals(1, service.notices().size());
        assertFalse(service.notices().getFirst().publicBroadcast());
    }

    @Test
    void activeHistoryExcludesExpiredPunishments() {
        PunishmentService service = new PunishmentService(protocolManager, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();
        service.punish(new PunishmentRequest(target, "Target", UUID.randomUUID(), "Staff", PunishmentType.TEMP_MUTE, "Spam", Instant.parse("2026-05-12T00:00:00Z"), false, "hub"));
        service.punish(new PunishmentRequest(target, "Target", UUID.randomUUID(), "Staff", PunishmentType.MUTE, "Spam", null, false, "hub"));
        assertEquals(2, service.history(target).size());
        assertEquals(1, service.active(target).size());
        assertEquals(PunishmentType.MUTE, service.active(target).getFirst().type());
    }
}

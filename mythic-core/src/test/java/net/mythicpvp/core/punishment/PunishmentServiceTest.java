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

    @Test
    void templatesCanBeAddedEditedRemovedAndResolvedByCategory() {
        PunishmentService service = new PunishmentService(protocolManager, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));

        PunishmentTemplate template = service.addTemplate(PunishmentCategory.BAN, "30d", "Cheating #1", "First cheating offense");

        assertEquals(PunishmentType.TEMP_BAN, template.type());
        assertEquals(1, service.templates(PunishmentCategory.BAN).size());
        assertTrue(service.editTemplate("Cheating #1", PunishmentCategory.BAN, "permanent", "Cheating #2", "Second cheating offense"));
        assertEquals(PunishmentType.BAN, service.template("Cheating #2").type());
        assertTrue(service.removeTemplate("Cheating #2"));
        assertTrue(service.templates().isEmpty());
    }

    @Test
    void punishmentExecutionStoresProofClearInventoryAndCanClearHistory() {
        PunishmentService service = new PunishmentService(protocolManager, Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC));
        UUID target = UUID.randomUUID();

        PunishmentRecord record = service.punish(new PunishmentRequest(target, "Target", UUID.randomUUID(), "Staff", PunishmentType.TEMP_BAN, "Cheating #1", "proof-link", Instant.parse("2026-06-12T00:00:00Z"), true, true, "skyblock"));

        assertEquals("proof-link", record.proof());
        assertTrue(record.clearInventory());
        assertEquals(1, service.clearHistory(target));
        assertTrue(service.history(target).isEmpty());
    }
}

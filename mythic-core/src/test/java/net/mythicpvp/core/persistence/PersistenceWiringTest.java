package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentRequest;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceWiringTest {

    private static final UUID TARGET = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STAFF = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void rankServiceLoadAndEditMirrorToGateway() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        RankService rankService = new RankService();
        rankService.setPersistence(gateway);

        rankService.register(rank("default", 1000));
        rankService.register(rank("admin", 10));

        assertTrue(rankService.setField("admin", "chat-prefix", "&c[ADMIN]"));
        assertTrue(rankService.addPermission("admin", "mythic.core.test"));
        assertTrue(rankService.removePermission("admin", "mythic.core.test"));

        long defines = gateway.calls.stream()
                .filter(c -> c instanceof CapturingPersistenceGateway.RankDefine)
                .count();
        assertEquals(5, defines, "every register() should mirror to gateway");

        boolean anySeeded = gateway.calls.stream()
                .filter(c -> c instanceof CapturingPersistenceGateway.RankDefine def && def.seeded())
                .findAny().isPresent();
        assertEquals(false, anySeeded, "direct register() must not mark seeded=true");
    }

    @Test
    void grantServiceMutationsMirrorToGateway() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        RankService rankService = new RankService();
        rankService.register(rank("default", 1000));
        rankService.register(rank("admin", 10));
        GrantService grantService = new GrantService(rankService, FIXED_CLOCK);
        grantService.setPersistence(gateway);

        RankGrant grant = grantService.grant(
                TARGET, "Notch", "admin", GrantDuration.parse("30d"),
                STAFF, "Console", "promotion");
        assertTrue(grantService.deactivate(grant.id()));
        assertTrue(grantService.removeInactive(grant.id()));

        UUID other = UUID.randomUUID();
        grantService.grant(other, "Other", "admin", GrantDuration.parse("permanent"),
                STAFF, "Console", "test");
        int cleared = grantService.clear(other);
        assertEquals(1, cleared);

        List<Object> calls = gateway.calls;

        assertEquals(5, calls.size(), "every mutation should fire one gateway call");
        assertTrue(calls.get(0) instanceof CapturingPersistenceGateway.GrantIssue);
        assertTrue(calls.get(1) instanceof CapturingPersistenceGateway.GrantDeactivate);
        assertTrue(calls.get(2) instanceof CapturingPersistenceGateway.GrantRemoveInactive);
        assertTrue(calls.get(3) instanceof CapturingPersistenceGateway.GrantIssue);
        assertTrue(calls.get(4) instanceof CapturingPersistenceGateway.GrantClear);

        CapturingPersistenceGateway.GrantIssue first = (CapturingPersistenceGateway.GrantIssue) calls.get(0);
        assertEquals(TARGET, first.grant().targetUuid());
        assertEquals("admin", first.grant().rankId());

        CapturingPersistenceGateway.GrantClear clear = (CapturingPersistenceGateway.GrantClear) calls.get(4);
        assertEquals(other, clear.target());
    }

    @Test
    void punishmentServiceMutationsMirrorToGateway() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();

        PunishmentService service = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        service.setPersistence(gateway);

        service.seedTemplate(PunishmentCategory.WARN, "permanent",
                "General Warning", "Used for minor rule reminders.");

        service.addTemplate(PunishmentCategory.MUTE, "1d", "Chat #1", "First chat offense.");

        assertTrue(service.editTemplate("Chat #1", PunishmentCategory.MUTE, "2d", "Chat #1", "Updated."));

        assertTrue(service.editTemplate("Chat #1", PunishmentCategory.MUTE, "2d", "Chat #2", "Renamed."));

        assertTrue(service.removeTemplate("Chat #2"));

        PunishmentRequest request = new PunishmentRequest(
                TARGET, "Notch", STAFF, "Admin",
                PunishmentType.TEMP_BAN, "exploit", "screenshot.png",
                Instant.parse("2026-06-14T12:00:00Z"), false, true, "hub-1");
        long punishmentId = service.punish(request).id();
        assertTrue(service.pardon(punishmentId, STAFF, "appeal accepted"));

        assertEquals(8, gateway.calls.size());

        CapturingPersistenceGateway.TemplateUpsert seeded =
                (CapturingPersistenceGateway.TemplateUpsert) gateway.calls.get(0);
        assertEquals(true, seeded.seeded());
        CapturingPersistenceGateway.TemplateUpsert runtime =
                (CapturingPersistenceGateway.TemplateUpsert) gateway.calls.get(1);
        assertEquals(false, runtime.seeded());

        Object punishCall = gateway.calls.stream()
                .filter(c -> c instanceof CapturingPersistenceGateway.PunishIssue)
                .findFirst().orElseThrow();
        CapturingPersistenceGateway.PunishIssue issued =
                (CapturingPersistenceGateway.PunishIssue) punishCall;
        assertEquals("Notch", issued.record().targetName());
        assertEquals("Admin", issued.record().staffName());
        assertEquals("hub-1", issued.record().server());
        assertEquals(true, issued.record().clearInventory());
        assertEquals(false, issued.record().silent());
    }

    @Test
    void noopGatewayIsTheDefault() {

        RankService rankService = new RankService();

        rankService.register(rank("default", 1000));

        assertSame(NoopPersistenceGateway.INSTANCE, NoopPersistenceGateway.INSTANCE);
    }

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE, "&7", "", weight, false, false, "",
                List.of(), "&7", "%chat_prefix%%player%: %message%",
                "&7", "%tab_prefix%%player%",
                "&7", "%nametag_prefix%%player%",
                CoreRank.SCOPE_GLOBAL);
    }
}

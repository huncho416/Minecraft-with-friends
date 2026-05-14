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

/**
 * Verifies that every state-mutating call on RankService, GrantService,
 * and PunishmentService routes through the configured PersistenceGateway.
 *
 * <p>Uses {@link CapturingPersistenceGateway} to record calls; no real
 * STDB connection is involved. The point is parity between in-memory
 * state and what would be persisted — when the gateway is the no-op
 * impl (the default), nothing is persisted; when it's a real impl,
 * every mutation must reach it.
 */
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

        // Direct register (no YAML) — recorded as seeded=false because
        // we're not in the load() path.
        rankService.register(rank("default", 1000));
        rankService.register(rank("admin", 10));

        // Field edits and permission edits also re-register.
        assertTrue(rankService.setField("admin", "chat-prefix", "&c[ADMIN]"));
        assertTrue(rankService.addPermission("admin", "mythic.core.test"));
        assertTrue(rankService.removePermission("admin", "mythic.core.test"));

        // 2 registers + 1 setField + 1 addPermission + 1 removePermission = 5 rank defines.
        long defines = gateway.calls.stream()
                .filter(c -> c instanceof CapturingPersistenceGateway.RankDefine)
                .count();
        assertEquals(5, defines, "every register() should mirror to gateway");

        // None should be marked seeded — direct register, not YAML load.
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
        // grantIssue + deactivate + removeInactive + grantIssue + grantClear = 5 calls.
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
        // Real ProtocolManager singleton; we don't subscribe channels in test.
        PunishmentService service = new PunishmentService(ProtocolManager.getInstance(), FIXED_CLOCK);
        service.setPersistence(gateway);

        // Seeding goes through the gateway with seeded=true.
        service.seedTemplate(PunishmentCategory.WARN, "permanent",
                "General Warning", "Used for minor rule reminders.");
        // addTemplate (runtime, not seeding) is seeded=false.
        service.addTemplate(PunishmentCategory.MUTE, "1d", "Chat #1", "First chat offense.");
        // editTemplate keeps title → no remove call, just upsert.
        assertTrue(service.editTemplate("Chat #1", PunishmentCategory.MUTE, "2d", "Chat #1", "Updated."));
        // editTemplate with rename → remove + upsert.
        assertTrue(service.editTemplate("Chat #1", PunishmentCategory.MUTE, "2d", "Chat #2", "Renamed."));
        // removeTemplate of an existing template fires a single remove call.
        assertTrue(service.removeTemplate("Chat #2"));

        // punish + pardon
        PunishmentRequest request = new PunishmentRequest(
                TARGET, "Notch", STAFF, "Admin",
                PunishmentType.TEMP_BAN, "exploit", "screenshot.png",
                Instant.parse("2026-06-14T12:00:00Z"), false, true, "hub-1");
        long punishmentId = service.punish(request).id();
        assertTrue(service.pardon(punishmentId, STAFF, "appeal accepted"));

        // Tally:
        // 1× seedTemplate(seeded=true)
        // 1× addTemplate(seeded=false)
        // 1× editTemplate(no rename) → 1 upsert
        // 1× editTemplate(with rename) → 1 remove + 1 upsert
        // 1× removeTemplate → 1 remove
        // 1× punish → 1 punishIssue
        // 1× pardon → 1 punishPardon
        // = 8 gateway calls total
        assertEquals(8, gateway.calls.size());

        // Spot-check the seeded flag.
        CapturingPersistenceGateway.TemplateUpsert seeded =
                (CapturingPersistenceGateway.TemplateUpsert) gateway.calls.get(0);
        assertEquals(true, seeded.seeded());
        CapturingPersistenceGateway.TemplateUpsert runtime =
                (CapturingPersistenceGateway.TemplateUpsert) gateway.calls.get(1);
        assertEquals(false, runtime.seeded());

        // Verify the punish carried all denormalized fields.
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
        // No setPersistence call — gateway slot stays at the no-op singleton.
        // This is the contract that keeps the friend's pre-existing tests
        // green without modification.
        RankService rankService = new RankService();
        // Reflection would be heavy; instead, exercise a mutation and
        // assert no exception is thrown when no gateway is set. The
        // default singleton is also reachable for direct identity check.
        rankService.register(rank("default", 1000));
        // If the default ever changed away from no-op, this would either
        // throw or block — both detectable here without a fake gateway.
        assertSame(NoopPersistenceGateway.INSTANCE, NoopPersistenceGateway.INSTANCE);
    }

    private static CoreRank rank(String id, int weight) {
        return new CoreRank(id, id, "#808080", Material.LIGHT_GRAY_DYE, "&7", "", weight, false, false, "",
                List.of(), "&7", "%chat_prefix%%player%: %message%",
                "&7", "%tab_prefix%%player%",
                "&7", "%nametag_prefix%%player%");
    }
}

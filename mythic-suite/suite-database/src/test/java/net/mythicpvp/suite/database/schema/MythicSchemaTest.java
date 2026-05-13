package net.mythicpvp.suite.database.schema;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mythicpvp.suite.database.SpacetimeConnection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the typed wrapper produces the exact on-wire shape STDB
 * expects: positional JSON-array args in the same order as the Rust
 * reducer signature.
 *
 * <p>We don't open a real WebSocket — we use {@link
 * SpacetimeConnection#reducerMessage} which performs the same Gson
 * serialization the live path uses.
 */
class MythicSchemaTest {

    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STAFF = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Gson GSON = new Gson();

    private final SpacetimeConnection conn = new SpacetimeConnection("http://localhost:3000", "mythicpvp");

    @Test
    void hyphenatedUuidMatchesRustExpectation() {
        assertEquals("11111111-1111-1111-1111-111111111111", MythicSchema.hyphenated(PLAYER));
    }

    @Test
    void registryAnnouncePackagesPositionalArgs() {
        JsonArray args = encodeArgs(
                ReducerNames.REGISTRY_ANNOUNCE,
                "hub-1", ServerRole.HUB.wireValue(), "us-east",
                "hub:25565", 200, SchemaVersion.CURRENT);
        assertEquals(6, args.size());
        assertEquals("hub-1", args.get(0).getAsString());
        assertEquals("HUB", args.get(1).getAsString());
        assertEquals("us-east", args.get(2).getAsString());
        assertEquals("hub:25565", args.get(3).getAsString());
        assertEquals(200, args.get(4).getAsInt());
        assertEquals(SchemaVersion.CURRENT, args.get(5).getAsInt());
    }

    @Test
    void sessionLoginShape() {
        JsonArray args = encodeArgs(
                ReducerNames.SESSION_LOGIN,
                MythicSchema.hyphenated(PLAYER), "Notch", "hub-1",
                42L, "ab12cd", "us-east");
        assertEquals(6, args.size());
        assertEquals(MythicSchema.hyphenated(PLAYER), args.get(0).getAsString());
        assertEquals("Notch", args.get(1).getAsString());
        assertEquals(42L, args.get(3).getAsLong());
    }

    @Test
    void economyAdjustRejectsZeroAmount() {
        MythicSchema schema = new MythicSchema(conn);
        assertThrows(IllegalArgumentException.class, () ->
                schema.economyAdjust(PLAYER, StdbCurrency.COINS, 0, "TEST", ""));
    }

    @Test
    void economyTransferRejectsSelfAndNonPositive() {
        MythicSchema schema = new MythicSchema(conn);
        assertThrows(IllegalArgumentException.class, () ->
                schema.economyTransfer(PLAYER, PLAYER, StdbCurrency.COINS, 10, ""));
        assertThrows(IllegalArgumentException.class, () ->
                schema.economyTransfer(PLAYER, STAFF, StdbCurrency.COINS, 0, ""));
        assertThrows(IllegalArgumentException.class, () ->
                schema.economyTransfer(PLAYER, STAFF, StdbCurrency.COINS, -5, ""));
    }

    @Test
    void economyRollbackRejectsBadWindow() {
        MythicSchema schema = new MythicSchema(conn);
        assertThrows(IllegalArgumentException.class, () ->
                schema.economyRollback(PLAYER, 100, 100, "test"));
        assertThrows(IllegalArgumentException.class, () ->
                schema.economyRollback(PLAYER, 200, 100, "test"));
    }

    @Test
    void friendRequestRejectsSelf() {
        MythicSchema schema = new MythicSchema(conn);
        assertThrows(IllegalArgumentException.class, () ->
                schema.friendRequest(PLAYER, PLAYER));
    }

    @Test
    void punishIssueArgsCarryEnumWireValue() {
        JsonArray args = encodeArgs(
                ReducerNames.PUNISH_ISSUE,
                MythicSchema.hyphenated(PLAYER),
                MythicSchema.hyphenated(STAFF),
                PunishmentKind.TEMP_BAN.wireValue(),
                "exploit", "https://evidence", 3600L);
        assertEquals("TEMP_BAN", args.get(2).getAsString());
        assertEquals(3600L, args.get(5).getAsLong());
    }

    @Test
    void allEnumWireValuesMatchRustConstants() {
        // Spot-check parity: a typo in either side breaks the round-trip.
        assertEquals("COINS", StdbCurrency.COINS.wireValue());
        assertEquals("PERMA_BAN", PunishmentKind.PERMA_BAN.wireValue());
        assertEquals("SKYBLOCK", ServerRole.SKYBLOCK.wireValue());
        assertEquals("HEALTHY", ServerStatus.HEALTHY.wireValue());
        assertEquals("CHAT_TAG", StdbCosmeticType.CHAT_TAG.wireValue());

        assertEquals(StdbCurrency.GEMS, StdbCurrency.fromWire("GEMS"));
        assertEquals(PunishmentKind.MUTE, PunishmentKind.fromWire("MUTE"));
        assertEquals(ServerRole.HUB, ServerRole.fromWire("HUB"));
        assertEquals(ServerStatus.DRAINING, ServerStatus.fromWire("DRAINING"));
        assertEquals(StdbCosmeticType.HAT, StdbCosmeticType.fromWire("HAT"));

        assertEquals(null, StdbCurrency.fromWire("BITCOIN"));
    }

    @Test
    void schemaVersionParserFindsVersion() {
        String payload = "{\"id\":0,\"schema_version\":1,\"initialized_at\":12345}";
        assertEquals(Integer.valueOf(1), SchemaVersion.parseSchemaVersion(payload));
    }

    @Test
    void schemaVersionParserHandlesWhitespaceAndAbsence() {
        assertEquals(Integer.valueOf(7),
                SchemaVersion.parseSchemaVersion("{ \"schema_version\" :   7 }"));
        assertEquals(null, SchemaVersion.parseSchemaVersion("{\"id\":0}"));
    }

    @Test
    void schemaVersionConstantMatchesCurrentWireExpectation() {
        // Bump in lockstep with mythic-cord/stdb/src/lib.rs::SCHEMA_VERSION.
        assertEquals(1, SchemaVersion.CURRENT);
    }

    /**
     * Extract the {@code args} array from the wire envelope. The connection
     * always wraps reducer calls in {@code {type, requestId, reducer, args}}.
     */
    private JsonArray encodeArgs(String reducer, Object... args) {
        String wire = conn.reducerMessage(reducer, args, "test-req");
        JsonObject envelope = GSON.fromJson(wire, JsonObject.class);
        assertNotNull(envelope);
        assertEquals("call", envelope.get("type").getAsString());
        assertEquals(reducer, envelope.get("reducer").getAsString());
        JsonElement argsElement = envelope.get("args");
        assertTrue(argsElement.isJsonArray(),
                "expected positional args array, got: " + argsElement);
        return argsElement.getAsJsonArray();
    }
}

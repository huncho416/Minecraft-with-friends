package net.mythicpvp.suite.database.schema;

import com.google.gson.Gson;
import net.mythicpvp.suite.database.schema.dto.PlayerRow;
import net.mythicpvp.suite.database.schema.dto.PunishmentRow;
import net.mythicpvp.suite.database.schema.dto.ServerEntryRow;
import net.mythicpvp.suite.database.schema.dto.TransactionRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies DTO records deserialize correctly from JSON payloads matching
 * the Rust struct field names. Gson uses record component names verbatim,
 * so snake_case in the record definitions is what STDB emits.
 */
class DtoRoundTripTest {

    private final Gson gson = new Gson();

    @Test
    void playerRowRoundTrip() {
        String json = "{"
                + "\"uuid\":\"11111111-1111-1111-1111-111111111111\","
                + "\"username\":\"Notch\","
                + "\"username_lower\":\"notch\","
                + "\"rank\":\"\","
                + "\"coins\":100,"
                + "\"points\":50,"
                + "\"gems\":0,"
                + "\"current_shard\":\"hub-1\","
                + "\"region\":\"us-east\","
                + "\"online\":true,"
                + "\"first_join\":111,"
                + "\"last_seen\":222,"
                + "\"playtime_seconds\":3600"
                + "}";
        PlayerRow row = gson.fromJson(json, PlayerRow.class);
        assertEquals("Notch", row.username());
        assertEquals("notch", row.username_lower());
        assertEquals(100, row.coins());
        assertTrue(row.online());
        assertEquals("hub-1", row.current_shard());
        assertEquals(3600, row.playtime_seconds());
    }

    @Test
    void punishmentRowSentinelExpiry() {
        String json = "{"
                + "\"id\":7,"
                + "\"target_uuid\":\"11111111-1111-1111-1111-111111111111\","
                + "\"staff_uuid\":\"22222222-2222-2222-2222-222222222222\","
                + "\"kind\":\"PERMA_BAN\","
                + "\"reason\":\"exploit\","
                + "\"evidence\":\"\","
                + "\"issued_at\":100,"
                + "\"expires_at_micros\":0,"
                + "\"active\":true,"
                + "\"pardoned_by\":\"\","
                + "\"pardoned_at_micros\":0,"
                + "\"pardon_reason\":\"\""
                + "}";
        PunishmentRow row = gson.fromJson(json, PunishmentRow.class);
        assertEquals(PunishmentKind.PERMA_BAN, PunishmentKind.fromWire(row.kind()));
        assertEquals(0L, row.expires_at_micros(), "0 is the sentinel for 'no expiry'");
        assertTrue(row.active());
    }

    @Test
    void serverEntryRowDecodesNumericTypes() {
        String json = "{"
                + "\"shard_id\":\"sb-1\","
                + "\"role\":\"SKYBLOCK\","
                + "\"region\":\"us-east\","
                + "\"status\":\"HEALTHY\","
                + "\"address\":\"sb-1:25565\","
                + "\"max_players\":150,"
                + "\"player_count\":42,"
                + "\"tps\":19.8,"
                + "\"heap_load\":0.65,"
                + "\"schema_version\":1,"
                + "\"started_at\":1000,"
                + "\"last_heartbeat\":2000"
                + "}";
        ServerEntryRow row = gson.fromJson(json, ServerEntryRow.class);
        assertEquals(ServerRole.SKYBLOCK, ServerRole.fromWire(row.role()));
        assertEquals(ServerStatus.HEALTHY, ServerStatus.fromWire(row.status()));
        assertEquals(42, row.player_count());
        assertEquals(19.8f, row.tps(), 0.001f);
        assertEquals(0.65f, row.heap_load(), 0.001f);
    }

    @Test
    void transactionRowMarksRollback() {
        String json = "{"
                + "\"id\":99,"
                + "\"player_uuid\":\"11111111-1111-1111-1111-111111111111\","
                + "\"currency\":\"COINS\","
                + "\"amount\":-500,"
                + "\"balance_after\":1500,"
                + "\"source\":\"ROLLBACK\","
                + "\"reference\":\"orig_id=42 dupe-cleanup\","
                + "\"is_rollback\":true,"
                + "\"at\":3000"
                + "}";
        TransactionRow row = gson.fromJson(json, TransactionRow.class);
        assertTrue(row.is_rollback());
        assertEquals(-500, row.amount());
        assertEquals(StdbCurrency.COINS, StdbCurrency.fromWire(row.currency()));
    }

    @Test
    void playerRowSerializesBackToSameShape() {
        PlayerRow row = new PlayerRow(
                "11111111-1111-1111-1111-111111111111", "Notch", "notch",
                "vip", 1, 2, 3, "hub-1", "eu-west", false,
                10, 20, 30);
        String json = gson.toJson(row);
        // Round-trip parity — if Gson ever renames record fields, this catches it.
        PlayerRow back = gson.fromJson(json, PlayerRow.class);
        assertEquals(row, back);
        assertTrue(json.contains("\"username_lower\":\"notch\""));
        assertFalse(json.contains("usernameLower"));
    }
}

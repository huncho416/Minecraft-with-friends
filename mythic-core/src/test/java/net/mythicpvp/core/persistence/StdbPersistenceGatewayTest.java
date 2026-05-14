package net.mythicpvp.core.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StdbPersistenceGatewayTest {

    @Test
    void zeroExpiryMapsToZeroDuration() {
        assertEquals(0L, StdbPersistenceGateway.durationSecondsFromExpiry(1000L, 0L));
        assertEquals(0L, StdbPersistenceGateway.durationSecondsFromExpiry(1000L, -5L));
    }

    @Test
    void positiveExpiryMapsToSecondsRelativeToCreation() {
        long createdMillis = 1_000_000L;
        long expiresMillis = createdMillis + 60_000L;
        assertEquals(60L, StdbPersistenceGateway.durationSecondsFromExpiry(createdMillis, expiresMillis));
    }

    @Test
    void subSecondExpiryClampsToOneSecond() {

        long createdMillis = 1_000L;
        long expiresMillis = 1_500L;
        assertEquals(1L, StdbPersistenceGateway.durationSecondsFromExpiry(createdMillis, expiresMillis));
    }

    @Test
    void expiryBeforeCreationClampsToZero() {

        assertEquals(0L, StdbPersistenceGateway.durationSecondsFromExpiry(2000L, 1000L));
    }

    @Test
    void microsToMillisPreservesPermanentSentinel() {

        assertEquals(0L, StdbPersistenceGateway.microsToMillis(0L));
    }

    @Test
    void microsToMillisDividesByThousand() {
        assertEquals(1L, StdbPersistenceGateway.microsToMillis(1_000L));
        assertEquals(1_700_000_000_000L,
                StdbPersistenceGateway.microsToMillis(1_700_000_000_000_000L));
    }

    @Test
    void coreRankRoundTripsThroughDtoConversion() {
        net.mythicpvp.suite.database.schema.dto.RankDefinitionRow row =
                new net.mythicpvp.suite.database.schema.dto.RankDefinitionRow(
                        "vip", "VIP", "#FFFF00", "YELLOW_DYE",
                        "&e[VIP]", "", 100, false, true, "default",
                        "[\"mythic.vip.fly\",\"mythic.vip.color\"]",
                        "&e[VIP] ", "%chat_prefix%%player%&7: &f%message%",
                        "&e[VIP] ", "%tab_prefix%%player%",
                        "&e[VIP] ", "%nametag_prefix%%player%",
                        true, 1L, 2L);
        net.mythicpvp.core.rank.CoreRank rank = StdbPersistenceGateway.toCoreRank(row);
        assertEquals("vip", rank.id());
        assertEquals(100, rank.weight());
        assertEquals(true, rank.donator());
        assertEquals(2, rank.permissions().size(),
                "permissions_json must round-trip into a real list");
        assertEquals("mythic.vip.fly", rank.permissions().get(0));
    }

    @Test
    void coreRankFallsBackOnUnknownDye() {
        net.mythicpvp.suite.database.schema.dto.RankDefinitionRow row =
                new net.mythicpvp.suite.database.schema.dto.RankDefinitionRow(
                        "x", "X", "#000", "NOT_A_REAL_MATERIAL",
                        "", "", 0, false, false, "",
                        "[]", "", "", "", "", "", "",
                        false, 0L, 0L);

        assertEquals(org.bukkit.Material.LIGHT_GRAY_DYE,
                StdbPersistenceGateway.toCoreRank(row).dye());
    }

    @Test
    void rankGrantRoundTripsThroughDtoConversion() {
        net.mythicpvp.suite.database.schema.dto.RankGrantRow row =
                new net.mythicpvp.suite.database.schema.dto.RankGrantRow(
                        7L, "11111111-1111-1111-1111-111111111111", "Notch",
                        "vip", "22222222-2222-2222-2222-222222222222", "Admin",
                        "purchased", "PURCHASE",
                        2_000_000_000_000_000L, 0L, true);
        net.mythicpvp.core.rank.RankGrant grant = StdbPersistenceGateway.toRankGrant(row);
        assertEquals(7L, grant.id());
        assertEquals("vip", grant.rankId());
        assertEquals(true, grant.permanent(), "0 expires_at_micros = permanent");
        assertEquals(2_000_000_000_000L, grant.createdAtMillis());
    }

    @Test
    void punishmentRecordMapsActiveFalseToPardonedTrue() {

        net.mythicpvp.suite.database.schema.dto.PunishmentRow row =
                new net.mythicpvp.suite.database.schema.dto.PunishmentRow(
                        99L, "11111111-1111-1111-1111-111111111111", "Notch",
                        "22222222-2222-2222-2222-222222222222", "Admin",
                        "BAN", "exploit", "screenshot.png",
                        2_000_000_000_000_000L, 0L,
                        false, false, true, "hub-1",
                        "system", 0L, "");
        net.mythicpvp.core.punishment.PunishmentRecord rec =
                StdbPersistenceGateway.toPunishmentRecord(row);
        assertEquals(true, rec.pardoned());
        assertEquals(net.mythicpvp.core.punishment.PunishmentType.BAN, rec.type());
        assertEquals(true, rec.clearInventory());
    }

    @Test
    void punishmentRecordHandlesUnknownKindAsBan() {

        net.mythicpvp.suite.database.schema.dto.PunishmentRow row =
                new net.mythicpvp.suite.database.schema.dto.PunishmentRow(
                        1L, "11111111-1111-1111-1111-111111111111", "Notch",
                        "22222222-2222-2222-2222-222222222222", "Admin",
                        "FUTURE_KIND_WE_DONT_KNOW", "", "",
                        0L, 0L, true, false, false, "hub-1",
                        "", 0L, "");
        net.mythicpvp.core.punishment.PunishmentRecord rec =
                StdbPersistenceGateway.toPunishmentRecord(row);
        assertEquals(net.mythicpvp.core.punishment.PunishmentType.BAN, rec.type(),
                "unknown wire kind must fall back, not throw");
    }
}

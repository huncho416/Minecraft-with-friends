package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code cosmetic_equipped} table row (one per slot). */
public record EquippedSlotRow(
        long id,
        String player_uuid,
        String cosmetic_type,
        String cosmetic_id,
        long equipped_at
) {}

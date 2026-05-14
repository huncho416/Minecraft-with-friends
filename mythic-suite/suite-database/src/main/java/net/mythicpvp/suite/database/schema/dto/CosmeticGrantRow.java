package net.mythicpvp.suite.database.schema.dto;

public record CosmeticGrantRow(
        long id,
        String player_uuid,
        String cosmetic_id,
        String cosmetic_type,
        String source,
        String reference,
        long granted_at
) {}

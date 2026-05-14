package net.mythicpvp.suite.database.schema.dto;

public record RankGrantRow(
        long id,
        String target_uuid,
        String target_name,
        String rank_id,
        String executor_uuid,
        String executor_name,
        String reason,
        String source,
        long created_at,
        long expires_at_micros,
        boolean active
) {}

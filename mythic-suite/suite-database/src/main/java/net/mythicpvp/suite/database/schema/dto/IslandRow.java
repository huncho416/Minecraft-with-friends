package net.mythicpvp.suite.database.schema.dto;

public record IslandRow(
        String island_id,
        String owner_uuid,
        String shard_id,
        int level,
        long total_points,
        String size_tier,
        long created_at,
        long last_visited
) {}

package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code server_registry} table row. */
public record ServerEntryRow(
        String shard_id,
        String role,
        String region,
        String status,
        String address,
        int max_players,
        int player_count,
        float tps,
        float heap_load,
        int schema_version,
        long started_at,
        long last_heartbeat
) {}

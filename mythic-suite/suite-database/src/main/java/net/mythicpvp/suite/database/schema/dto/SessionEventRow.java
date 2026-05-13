package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code session_history} table row. */
public record SessionEventRow(
        long id,
        String player_uuid,
        String shard_id,
        String event_type,
        String reason,
        long at
) {}

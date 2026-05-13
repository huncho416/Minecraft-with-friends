package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code players} table row.
 *
 * <p>Field names match the Rust struct exactly — Gson's default naming
 * policy is identity, so payloads round-trip without a custom deserializer.
 * Timestamps are microseconds since Unix epoch.
 */
public record PlayerRow(
        String uuid,
        String username,
        String username_lower,
        String rank,
        long coins,
        long points,
        long gems,
        String current_shard,
        String region,
        boolean online,
        long first_join,
        long last_seen,
        long playtime_seconds
) {}

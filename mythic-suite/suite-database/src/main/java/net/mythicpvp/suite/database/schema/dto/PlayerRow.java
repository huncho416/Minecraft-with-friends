package net.mythicpvp.suite.database.schema.dto;

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

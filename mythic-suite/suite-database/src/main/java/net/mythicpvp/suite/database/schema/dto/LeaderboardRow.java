package net.mythicpvp.suite.database.schema.dto;

public record LeaderboardRow(
        long id,
        String board,
        String timeframe,
        String player_uuid,
        String username,
        long score,
        int rank,
        long computed_at
) {}

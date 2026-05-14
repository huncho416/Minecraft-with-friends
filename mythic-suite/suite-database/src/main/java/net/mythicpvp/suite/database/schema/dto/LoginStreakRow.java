package net.mythicpvp.suite.database.schema.dto;

public record LoginStreakRow(
        long id,
        String player_uuid,
        long last_login_at,
        int current_streak
) {}

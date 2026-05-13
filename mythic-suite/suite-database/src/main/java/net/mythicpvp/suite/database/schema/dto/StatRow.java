package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code stats} table row. */
public record StatRow(
        long id,
        String player_uuid,
        String stat,
        long value_daily,
        long value_weekly,
        long value_alltime,
        long last_updated
) {}

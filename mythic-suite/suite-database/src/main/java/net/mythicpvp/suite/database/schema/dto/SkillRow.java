package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code skills} table row. */
public record SkillRow(
        long id,
        String player_uuid,
        String skill,
        long xp,
        int level,
        long last_updated
) {}

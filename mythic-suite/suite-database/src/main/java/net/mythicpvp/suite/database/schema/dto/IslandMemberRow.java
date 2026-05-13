package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code island_members} table row. */
public record IslandMemberRow(
        long id,
        String island_id,
        String player_uuid,
        String role,
        long joined_at
) {}

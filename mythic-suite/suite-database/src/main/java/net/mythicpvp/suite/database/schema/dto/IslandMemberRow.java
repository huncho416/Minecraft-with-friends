package net.mythicpvp.suite.database.schema.dto;

public record IslandMemberRow(
        long id,
        String island_id,
        String player_uuid,
        String role,
        long joined_at
) {}

package net.mythicpvp.suite.database.schema.dto;

public record PartyMemberRow(
        long id,
        long party_id,
        String player_uuid,
        long joined_at
) {}

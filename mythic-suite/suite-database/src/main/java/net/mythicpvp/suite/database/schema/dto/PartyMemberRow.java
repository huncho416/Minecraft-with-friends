package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code party_members} table row. */
public record PartyMemberRow(
        long id,
        long party_id,
        String player_uuid,
        long joined_at
) {}

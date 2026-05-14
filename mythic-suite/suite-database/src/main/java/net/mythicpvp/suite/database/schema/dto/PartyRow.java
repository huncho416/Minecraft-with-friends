package net.mythicpvp.suite.database.schema.dto;

public record PartyRow(
        long id,
        String leader_uuid,
        long created_at
) {}

package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code parties} table row. */
public record PartyRow(
        long id,
        String leader_uuid,
        long created_at
) {}

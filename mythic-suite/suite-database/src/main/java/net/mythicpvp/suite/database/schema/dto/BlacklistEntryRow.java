package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code punishment_blacklist} table row. Soft-delete pattern:
 * inactive entries stay in the table for audit; an entry is "in effect"
 * iff {@code active == true}.
 */
public record BlacklistEntryRow(
        long id,
        String target_uuid,
        String target_name,
        String staff_uuid,
        String staff_name,
        String reason,
        boolean active,
        long created_at,
        long revoked_at_micros,
        String revoked_by,
        String revoke_reason
) {}

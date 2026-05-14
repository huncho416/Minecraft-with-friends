package net.mythicpvp.suite.database.schema.dto;

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

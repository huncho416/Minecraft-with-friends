package net.mythicpvp.suite.database.schema.dto;

public record PunishmentRow(
        long id,
        String target_uuid,
        String target_name,
        String staff_uuid,
        String staff_name,
        String kind,
        String reason,
        String proof,
        long issued_at,
        long expires_at_micros,
        boolean active,
        boolean silent,
        boolean clear_inventory,
        String server,
        String pardoned_by,
        long pardoned_at_micros,
        String pardon_reason
) {}

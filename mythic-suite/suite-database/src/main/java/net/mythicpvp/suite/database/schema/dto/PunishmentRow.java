package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code punishments} table row.
 *
 * <p>{@code expires_at_micros == 0} means "no expiry" (permanent). The
 * pardon fields are populated when {@code active=false}.
 */
public record PunishmentRow(
        long id,
        String target_uuid,
        String staff_uuid,
        String kind,
        String reason,
        String evidence,
        long issued_at,
        long expires_at_micros,
        boolean active,
        String pardoned_by,
        long pardoned_at_micros,
        String pardon_reason
) {}

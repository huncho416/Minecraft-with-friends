package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code punishments} table row (schema v2).
 *
 * <p>{@code expires_at_micros == 0} means "no expiry" (permanent). The
 * pardon fields are populated when {@code active=false}.
 *
 * <p>Schema v2 changes from v1: added {@code target_name},
 * {@code staff_name}, {@code silent}, {@code clear_inventory},
 * {@code server}; renamed {@code evidence} → {@code proof}.
 */
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

package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code rank_grants} table row.
 *
 * <p>{@code expires_at_micros == 0} means permanent. Inactive grants stay
 * in the table for history (Phase 3 PLAN line 683) until explicitly
 * removed via {@code grant_remove_inactive}.
 */
public record RankGrantRow(
        long id,
        String target_uuid,
        String target_name,
        String rank_id,
        String executor_uuid,
        String executor_name,
        String reason,
        String source,
        long created_at,
        long expires_at_micros,
        boolean active
) {}

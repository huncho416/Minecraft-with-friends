package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code punishment_templates} table row.
 *
 * <p>{@code title} is the natural key for {@code template_upsert} /
 * {@code template_remove}. {@code seeded=true} marks templates created
 * by YAML bootstrapping; the upsert reducer never flips this back to
 * false so manual edits stay distinguishable.
 */
public record PunishmentTemplateRow(
        long id,
        String title,
        String category,
        String duration,
        String information,
        boolean seeded,
        long created_at,
        long updated_at
) {}

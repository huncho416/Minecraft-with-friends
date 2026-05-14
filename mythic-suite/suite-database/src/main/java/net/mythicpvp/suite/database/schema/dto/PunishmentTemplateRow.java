package net.mythicpvp.suite.database.schema.dto;

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

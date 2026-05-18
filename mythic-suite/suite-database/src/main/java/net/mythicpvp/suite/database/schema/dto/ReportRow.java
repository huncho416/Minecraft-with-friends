package net.mythicpvp.suite.database.schema.dto;

public record ReportRow(
        long id,
        String reporter_uuid,
        String reporter_name,
        String target_uuid,
        String target_name,
        String category,
        String reporter_shard,
        long created_at,
        boolean resolved,
        String resolver_uuid,
        String resolver_name,
        String resolution,
        long resolved_at_micros
) {}

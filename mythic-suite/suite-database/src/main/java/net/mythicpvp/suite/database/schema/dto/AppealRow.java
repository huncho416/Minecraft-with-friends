package net.mythicpvp.suite.database.schema.dto;

public record AppealRow(
        long id,
        long punishment_id,
        String target_uuid,
        String message,
        String status,
        String reviewer_uuid,
        String review_notes,
        long created_at,
        long reviewed_at_micros
) {}

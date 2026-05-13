package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code mail} table row.
 *
 * <p>{@code attachments_json} is a free-form JSON blob — keep its shape
 * documented in {@code mythic-core}'s mail handler, not here.
 */
public record MailRow(
        long id,
        String recipient_uuid,
        String sender_uuid,
        String subject,
        String body,
        String attachments_json,
        boolean read,
        long sent_at,
        long read_at_micros
) {}

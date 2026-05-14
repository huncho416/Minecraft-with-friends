package net.mythicpvp.suite.database.schema.dto;

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

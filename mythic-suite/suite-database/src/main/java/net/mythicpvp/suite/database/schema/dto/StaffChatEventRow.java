package net.mythicpvp.suite.database.schema.dto;

public record StaffChatEventRow(
        long id,
        String channel,
        String sender_uuid,
        String sender_name,
        String sender_rank,
        String sender_rank_color,
        String sender_chat_prefix,
        String origin_shard,
        String message,
        long created_at
) {}

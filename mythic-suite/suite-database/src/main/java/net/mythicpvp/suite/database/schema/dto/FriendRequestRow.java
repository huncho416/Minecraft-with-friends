package net.mythicpvp.suite.database.schema.dto;

public record FriendRequestRow(
        long id,
        String from_uuid,
        String to_uuid,
        long created_at
) {}

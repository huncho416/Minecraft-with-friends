package net.mythicpvp.suite.database.schema.dto;

public record FriendRow(
        long id,
        String owner_uuid,
        String friend_uuid,
        long added_at
) {}

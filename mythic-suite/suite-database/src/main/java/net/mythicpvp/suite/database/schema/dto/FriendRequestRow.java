package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code friend_requests} table row. */
public record FriendRequestRow(
        long id,
        String from_uuid,
        String to_uuid,
        long created_at
) {}

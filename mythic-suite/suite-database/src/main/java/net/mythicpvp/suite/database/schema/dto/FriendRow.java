package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code friends} table row (one per direction). */
public record FriendRow(
        long id,
        String owner_uuid,
        String friend_uuid,
        long added_at
) {}

package net.mythicpvp.suite.database.schema.dto;

public record SessionRow(
        String player_uuid,
        String username,
        String shard_id,
        long proxy_session_id,
        String ip_hash,
        String region,
        boolean vanished,
        long login_at,
        long last_activity
) {}

package net.mythicpvp.suite.database.schema.dto;

public record TransferRequestRow(
        long id,
        String target_uuid,
        String target_name,
        String destination_shard,
        String requester_uuid,
        String requester_name,
        long created_at
) {}

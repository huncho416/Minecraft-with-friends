package net.mythicpvp.suite.database.schema.dto;

/** Mirrors the {@code transactions} table row. */
public record TransactionRow(
        long id,
        String player_uuid,
        String currency,
        long amount,
        long balance_after,
        String source,
        String reference,
        boolean is_rollback,
        long at
) {}

package net.mythicpvp.suite.database.schema.dto;

public record ModuleMetaRow(
        int id,
        int schema_version,
        long initialized_at,
        long last_migrated_at
) {}

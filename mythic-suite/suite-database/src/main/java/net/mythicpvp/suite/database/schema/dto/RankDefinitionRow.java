package net.mythicpvp.suite.database.schema.dto;

/**
 * Mirrors the {@code rank_definitions} table row.
 *
 * <p>{@code permissions_json} is a JSON-encoded string array because
 * SpacetimeDB doesn't natively expose {@code Vec<String>} columns.
 * Decode with {@code Gson().fromJson(permissions_json, String[].class)}.
 *
 * <p>{@code dye} is the Bukkit {@code Material} name as a string so the
 * DB layer doesn't import the Bukkit API.
 */
public record RankDefinitionRow(
        String id,
        String display_name,
        String color,
        String dye,
        String prefix,
        String suffix,
        int weight,
        boolean staff,
        boolean donator,
        String parent,
        String permissions_json,
        String chat_prefix,
        String chat_format,
        String tab_prefix,
        String tab_format,
        String nametag_prefix,
        String nametag_format,
        boolean seeded,
        long created_at,
        long updated_at
) {}

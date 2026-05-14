package net.mythicpvp.suite.database.schema.dto;

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

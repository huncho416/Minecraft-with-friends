package net.mythicpvp.core;

import org.jetbrains.annotations.NotNull;

public record ServerIdentity(@NotNull String id, @NotNull String type) {

    @NotNull
    public static ServerIdentity fromEnvironment() {
        String id = firstPresent("local", "MYTHIC_SERVER_ID", "SHARD_ID", "SERVER_ID", "mythic.server.id");
        String type = firstPresent("development", "MYTHIC_SERVER_TYPE", "SERVER_TYPE", "mythic.server.type");
        return new ServerIdentity(id, type);
    }

    @NotNull
    private static String firstPresent(@NotNull String fallback, @NotNull String... keys) {
        String value = null;
        for (String key : keys) {
            value = System.getenv(key);
            if (value == null || value.isBlank()) {
                value = System.getProperty(key);
            }
            if (value != null && !value.isBlank()) {
                break;
            }
        }
        return value == null || value.isBlank() ? fallback : value;
    }
}

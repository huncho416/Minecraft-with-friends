package net.mythicpvp.core.config;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class CoreMessages {

    private final ConfigText text;

    public CoreMessages(@NotNull ConfigText text) {
        this.text = text;
    }

    @NotNull
    public Component component(@NotNull String key, @NotNull String fallback) {
        return text.component(key, fallback);
    }

    @NotNull
    public Component component(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return text.component(key, fallback, placeholders);
    }

    @NotNull
    public String raw(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return text.raw(key, fallback, placeholders);
    }
}

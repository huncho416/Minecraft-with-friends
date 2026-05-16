package net.mythicpvp.core.config;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.hex.MythicHex;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class CoreMessages {

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

    @NotNull
    public Component codeOwned(@NotNull String fallback) {
        return text.codeOwned(fallback);
    }

    @NotNull
    public Component codeOwned(@NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return text.codeOwned(fallback, placeholders);
    }

    @NotNull
    public List<Component> list(@NotNull String key, @NotNull List<String> fallback) {
        return text.list(key, fallback).stream().map(MythicHex::colorize).toList();
    }
}

package net.mythicpvp.suite.config;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigText {

    private final MythicConfig config;
    private final String root;

    public ConfigText(@NotNull MythicConfig config) {
        this(config, "messages");
    }

    public ConfigText(@NotNull MythicConfig config, @NotNull String root) {
        this.config = config;
        this.root = root;
    }

    @NotNull
    public String raw(@NotNull String key, @NotNull String fallback) {
        String path = path(key);
        if (!config.contains(path)) {
            config.set(path, fallback);
            config.save();
        }
        return config.getString(path, fallback);
    }

    @NotNull
    public String raw(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        String text = raw(key, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    @NotNull
    public Component component(@NotNull String key, @NotNull String fallback) {
        return MythicHex.colorize(raw(key, fallback));
    }

    @NotNull
    public Component component(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return MythicHex.colorize(raw(key, fallback, placeholders));
    }

    @NotNull
    public List<String> list(@NotNull String key, @NotNull List<String> fallback) {
        String path = path(key);
        if (!config.contains(path)) {
            config.set(path, fallback);
            config.save();
        }
        return config.getStringList(path);
    }

    @NotNull
    public Map<String, String> section(@NotNull String key) {
        ConfigurationSection section = config.getConfig().getConfigurationSection(path(key));
        if (section == null) {
            return Map.of();
        }
        Map<String, String> values = new HashMap<>();
        for (String child : section.getKeys(false)) {
            values.put(child, section.getString(child, ""));
        }
        return values;
    }

    @NotNull
    private String path(@NotNull String key) {
        return root.isBlank() ? key : root + "." + key;
    }
}

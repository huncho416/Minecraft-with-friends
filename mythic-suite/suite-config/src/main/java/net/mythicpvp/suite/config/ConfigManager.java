package net.mythicpvp.suite.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, MythicConfig> configs = new HashMap<>();

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public MythicConfig getOrCreate(@NotNull String name) {
        return configs.computeIfAbsent(name, n -> new MythicConfig(plugin, n));
    }

    @NotNull
    public MythicConfig getMain() {
        return getOrCreate("config");
    }

    public void reloadAll() {
        configs.values().forEach(MythicConfig::reload);
    }

    public void saveAll() {
        configs.values().forEach(MythicConfig::save);
    }

    public void unregister(@NotNull String name) {
        configs.remove(name);
    }
}

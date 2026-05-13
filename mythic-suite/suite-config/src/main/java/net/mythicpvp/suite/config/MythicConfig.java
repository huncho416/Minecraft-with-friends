package net.mythicpvp.suite.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MythicConfig {

    private final JavaPlugin plugin;
    private final String fileName;
    private final File file;
    private FileConfiguration config;

    public MythicConfig(@NotNull JavaPlugin plugin, @NotNull String fileName) {
        this.plugin = plugin;
        this.fileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        this.file = new File(plugin.getDataFolder(), this.fileName);
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create config file: " + fileName, e);
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file: " + fileName, e);
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    @NotNull
    public FileConfiguration getConfig() {
        return config;
    }

    @NotNull
    public File getFile() {
        return file;
    }

    @Nullable
    public String getString(@NotNull String path) {
        return config.getString(path);
    }

    @NotNull
    public String getString(@NotNull String path, @NotNull String def) {
        return config.getString(path, def);
    }

    public int getInt(@NotNull String path, int def) {
        return config.getInt(path, def);
    }

    public long getLong(@NotNull String path, long def) {
        return config.getLong(path, def);
    }

    public double getDouble(@NotNull String path, double def) {
        return config.getDouble(path, def);
    }

    public boolean getBoolean(@NotNull String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }

    public void set(@NotNull String path, @Nullable Object value) {
        config.set(path, value);
    }

    public boolean contains(@NotNull String path) {
        return config.contains(path);
    }
}

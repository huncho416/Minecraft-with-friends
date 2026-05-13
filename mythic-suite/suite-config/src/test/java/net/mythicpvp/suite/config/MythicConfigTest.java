package net.mythicpvp.suite.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MythicConfigTest {

    @Test
    void createsSavesAndReloadsConfigFiles() throws Exception {
        JavaPlugin plugin = mock(JavaPlugin.class);
        File folder = Files.createTempDirectory("mythic-config").toFile();
        when(plugin.getDataFolder()).thenReturn(folder);
        when(plugin.getResource("settings.yml")).thenReturn(null);
        MythicConfig config = new MythicConfig(plugin, "settings");
        config.set("name", "Mythic");
        config.save();
        config.reload();
        assertTrue(config.getFile().exists());
        assertEquals("Mythic", config.getString("name"));
    }

    @Test
    void configTextWritesFallbacksAndReplacesPlaceholders() throws Exception {
        JavaPlugin plugin = mock(JavaPlugin.class);
        File folder = Files.createTempDirectory("mythic-text").toFile();
        when(plugin.getDataFolder()).thenReturn(folder);
        when(plugin.getResource("messages.yml")).thenReturn(null);
        MythicConfig config = new MythicConfig(plugin, "messages");
        ConfigText text = new ConfigText(config);
        assertEquals("Hello Alex", text.raw("welcome", "Hello %player%", java.util.Map.of("player", "Alex")));
        config.reload();
        assertEquals("Hello %player%", config.getString("messages.welcome"));
    }
}

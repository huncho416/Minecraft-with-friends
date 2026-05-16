package net.mythicpvp.core.note;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NoteStore {

    private final File file;
    private final Logger logger;

    public NoteStore(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @NotNull
    public LoadResult load() {
        if (!file.exists()) {
            return new LoadResult(List.of(), 0L);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        long maxId = yaml.getLong("sequence", 0L);
        List<PlayerNote> notes = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("notes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection n = section.getConfigurationSection(key);
                if (n == null) continue;
                try {
                    PlayerNote note = readNote(n);
                    notes.add(note);
                    if (note.id() > maxId) maxId = note.id();
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Skipping malformed note entry " + key, e);
                }
            }
        }
        return new LoadResult(notes, maxId);
    }

    public void save(long sequence, @NotNull List<PlayerNote> notes) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("sequence", sequence);
        for (PlayerNote note : notes) {
            String prefix = "notes." + note.id() + ".";
            yaml.set(prefix + "targetUuid", note.targetUuid().toString());
            yaml.set(prefix + "targetName", note.targetName());
            yaml.set(prefix + "authorUuid", note.authorUuid().toString());
            yaml.set(prefix + "authorName", note.authorName());
            yaml.set(prefix + "title", note.title());
            yaml.set(prefix + "body", note.body());
            yaml.set(prefix + "serverId", note.serverId());
            yaml.set(prefix + "createdAt", note.createdAt());
            yaml.set(prefix + "active", note.active());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save notes.yml", e);
        }
    }

    @NotNull
    private static PlayerNote readNote(@NotNull ConfigurationSection n) {
        long id = Long.parseLong(n.getName());
        UUID targetUuid = UUID.fromString(n.getString("targetUuid"));
        String targetName = n.getString("targetName", "?");
        UUID authorUuid = UUID.fromString(n.getString("authorUuid"));
        String authorName = n.getString("authorName", "?");
        String title = n.getString("title", "");
        String body = n.getString("body", "");
        String serverId = n.getString("serverId", "?");
        long createdAt = n.getLong("createdAt");
        PlayerNote note = new PlayerNote(id, targetUuid, targetName, authorUuid, authorName,
                title, body, serverId, createdAt);
        note.setActive(n.getBoolean("active", true));
        return note;
    }

    public record LoadResult(@NotNull List<PlayerNote> notes, long maxId) {
    }
}

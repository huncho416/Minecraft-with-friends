package net.mythicpvp.core.staffmode;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StaffStateStore {

    private final File file;
    private final Logger logger;
    private final Set<UUID> staffMode = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();

    public StaffStateStore(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public boolean wasInStaffMode(@NotNull UUID player) {
        synchronized (staffMode) {
            return staffMode.contains(player);
        }
    }

    public boolean wasVanished(@NotNull UUID player) {
        synchronized (vanished) {
            return vanished.contains(player);
        }
    }

    public void recordStaffMode(@NotNull UUID player, boolean inStaffMode) {
        synchronized (staffMode) {
            if (inStaffMode) staffMode.add(player); else staffMode.remove(player);
        }
        save();
    }

    public void recordVanish(@NotNull UUID player, boolean isVanished) {
        synchronized (vanished) {
            if (isVanished) vanished.add(player); else vanished.remove(player);
        }
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String s : yaml.getStringList("staff-mode")) {
            try { staffMode.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : yaml.getStringList("vanished")) {
            try { vanished.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        synchronized (staffMode) {
            yaml.set("staff-mode", staffMode.stream().map(UUID::toString).toList());
        }
        synchronized (vanished) {
            yaml.set("vanished", vanished.stream().map(UUID::toString).toList());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save staff-state.yml", e);
        }
    }
}

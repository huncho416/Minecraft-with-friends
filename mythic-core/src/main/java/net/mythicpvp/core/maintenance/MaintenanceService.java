package net.mythicpvp.core.maintenance;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class MaintenanceService {

    public static final String BYPASS_PERMISSION = "mythic.core.maintenance.bypass";

    private final Logger logger;
    private final Path stateFile;
    private final Path bypassFile;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final Set<UUID> bypassUuids = ConcurrentHashMap.newKeySet();

    public MaintenanceService(@NotNull Logger logger, @NotNull File dataFolder) {
        this.logger = logger;
        this.stateFile = new File(dataFolder, "maintenance.flag").toPath();
        this.bypassFile = new File(dataFolder, "maintenance-bypass.txt").toPath();
        loadFromDisk();
    }

    public boolean isActive() {
        return active.get();
    }

    public boolean toggle() {
        boolean next = !active.get();
        setActive(next);
        return next;
    }

    public void setActive(boolean value) {
        active.set(value);
        try {
            if (value) {
                Files.writeString(stateFile, "on");
            } else {
                Files.deleteIfExists(stateFile);
            }
        } catch (IOException e) {
            logger.warning("[maintenance] failed to persist state: " + e.getMessage());
        }
    }

    public boolean canBypass(@NotNull UUID uuid) {
        return bypassUuids.contains(uuid);
    }

    public boolean addBypass(@NotNull UUID uuid) {
        boolean added = bypassUuids.add(uuid);
        if (added) saveBypass();
        return added;
    }

    public boolean removeBypass(@NotNull UUID uuid) {
        boolean removed = bypassUuids.remove(uuid);
        if (removed) saveBypass();
        return removed;
    }

    @NotNull
    public Set<UUID> bypassUuids() {
        return Set.copyOf(bypassUuids);
    }

    private void loadFromDisk() {
        if (Files.exists(stateFile)) {
            active.set(true);
        }
        if (Files.exists(bypassFile)) {
            try {
                for (String line : Files.readAllLines(bypassFile)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    try {
                        bypassUuids.add(UUID.fromString(trimmed));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (IOException e) {
                logger.warning("[maintenance] failed to load bypass list: " + e.getMessage());
            }
        }
    }

    private void saveBypass() {
        try {
            Files.writeString(bypassFile,
                    bypassUuids.stream().map(UUID::toString).collect(Collectors.joining("\n")));
        } catch (IOException e) {
            logger.warning("[maintenance] failed to persist bypass list: " + e.getMessage());
        }
    }
}

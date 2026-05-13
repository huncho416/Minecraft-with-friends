package net.mythicpvp.suite.hologram;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramManager {

    private static final HologramManager INSTANCE = new HologramManager();
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    private HologramManager() {}

    @NotNull
    public static HologramManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    public Hologram create(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
        remove(id);
        Hologram hologram = new Hologram(id, location, lines);
        holograms.put(id, hologram);
        return hologram;
    }

    public void remove(@NotNull String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.destroy();
        }
    }

    public void removeAll() {
        holograms.values().forEach(Hologram::destroy);
        holograms.clear();
    }

    @NotNull
    public Map<String, Hologram> getAll() {
        return Collections.unmodifiableMap(holograms);
    }

    public static class Hologram {
        private final String id;
        private Location location;
        private final List<TextDisplay> displays = new ArrayList<>();
        private List<String> lines;

        Hologram(@NotNull String id, @NotNull Location location, @NotNull List<String> lines) {
            this.id = id;
            this.location = location;
            this.lines = new ArrayList<>(lines);
            spawn();
        }

        private void spawn() {
            destroy();
            double offset = 0;
            for (String line : lines) {
                Location loc = location.clone().add(0, offset, 0);
                TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
                display.text(MythicHex.colorize(line));
                display.setBillboard(Display.Billboard.CENTER);
                display.setSeeThrough(false);
                display.setShadowed(true);
                displays.add(display);
                offset -= 0.3;
            }
        }

        public void setLines(@NotNull List<String> lines) {
            this.lines = new ArrayList<>(lines);
            spawn();
        }

        public void setLine(int index, @NotNull String text) {
            if (index >= 0 && index < displays.size()) {
                displays.get(index).text(MythicHex.colorize(text));
            }
        }

        public void teleport(@NotNull Location location) {
            this.location = location;
            spawn();
        }

        public void destroy() {
            displays.forEach(d -> { if (d != null && !d.isDead()) d.remove(); });
            displays.clear();
        }

        @NotNull public String getId() { return id; }
        @NotNull public Location getLocation() { return location; }
    }
}

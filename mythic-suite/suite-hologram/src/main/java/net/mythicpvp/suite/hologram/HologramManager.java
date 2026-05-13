package net.mythicpvp.suite.hologram;

import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.packet.PacketAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
        return create(id, location, lines, false, false);
    }

    @NotNull
    public Hologram create(@NotNull String id, @NotNull Location location, @NotNull List<String> lines, boolean perPlayer, boolean leaderboard) {
        remove(id);
        Hologram hologram = new Hologram(id, location, lines, perPlayer, leaderboard);
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
        private final boolean perPlayer;
        private final boolean leaderboard;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        private final List<TextDisplay> displays = new ArrayList<>();
        private final List<String> animationFrames = new ArrayList<>();
        private Location location;
        private List<String> lines;
        private int frameIndex;

        Hologram(@NotNull String id, @NotNull Location location, @NotNull List<String> lines, boolean perPlayer, boolean leaderboard) {
            this.id = id;
            this.location = location;
            this.lines = new ArrayList<>(lines);
            this.perPlayer = perPlayer;
            this.leaderboard = leaderboard;
            if (!perPlayer) {
                spawn();
            }
            emitAll();
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

        public void showTo(@NotNull Player player) {
            viewers.add(player.getUniqueId());
            emit(player);
        }

        public void hideFrom(@NotNull Player player) {
            viewers.remove(player.getUniqueId());
        }

        public void setLines(@NotNull List<String> lines) {
            this.lines = new ArrayList<>(lines);
            if (!perPlayer) {
                spawn();
            }
            emitAll();
        }

        public void setLine(int index, @NotNull String text) {
            if (index < 0) {
                throw new IllegalArgumentException("Line index cannot be negative");
            }
            while (lines.size() <= index) {
                lines.add("");
            }
            lines.set(index, text);
            if (!perPlayer && index < displays.size()) {
                displays.get(index).text(MythicHex.colorize(text));
            }
            emitAll();
        }

        public void setAnimationFrames(@NotNull List<String> frames) {
            animationFrames.clear();
            animationFrames.addAll(frames);
            frameIndex = 0;
            emitAll();
        }

        public void tickAnimation() {
            if (animationFrames.isEmpty()) {
                return;
            }
            frameIndex = (frameIndex + 1) % animationFrames.size();
            emitAll();
        }

        public void teleport(@NotNull Location location) {
            this.location = location;
            if (!perPlayer) {
                spawn();
            }
            emitAll();
        }

        public void destroy() {
            displays.forEach(display -> {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
            });
            displays.clear();
            viewers.clear();
        }

        private void emitAll() {
            if (perPlayer) {
                viewers.stream().map(Bukkit::getPlayer).filter(player -> player != null).forEach(this::emit);
            } else {
                Bukkit.getOnlinePlayers().forEach(this::emit);
            }
        }

        private void emit(@NotNull Player player) {
            String frame = animationFrames.isEmpty() ? "" : animationFrames.get(frameIndex);
            PacketAction.send(player, new PacketAction.HologramState(
                    "hologram:" + id,
                    lines.stream().map(MythicHex::colorize).toList(),
                    leaderboard,
                    frame
            ));
        }

        @NotNull
        public String getId() {
            return id;
        }

        @NotNull
        public Location getLocation() {
            return location;
        }

        @NotNull
        public List<String> getLines() {
            return Collections.unmodifiableList(lines);
        }

        public boolean isPerPlayer() {
            return perPlayer;
        }

        public boolean isLeaderboard() {
            return leaderboard;
        }
    }
}

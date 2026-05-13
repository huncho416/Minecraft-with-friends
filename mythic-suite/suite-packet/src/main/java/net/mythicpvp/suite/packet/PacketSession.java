package net.mythicpvp.suite.packet;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PacketSession {

    private static final PacketSession INSTANCE = new PacketSession();
    private final Map<UUID, List<PacketAction>> actions = new ConcurrentHashMap<>();
    private volatile PacketRenderer renderer = (viewer, action) -> {};

    private PacketSession() {}

    @NotNull
    public static PacketSession getInstance() {
        return INSTANCE;
    }

    public void setRenderer(@NotNull PacketRenderer renderer) {
        this.renderer = renderer;
    }

    public void resetRenderer() {
        this.renderer = (viewer, action) -> {};
    }

    public void send(@NotNull Player viewer, @NotNull PacketAction action) {
        actions.computeIfAbsent(viewer.getUniqueId(), key -> new CopyOnWriteArrayList<>()).add(action);
        renderer.render(viewer, action);
    }

    @NotNull
    public List<PacketAction> getActions(@NotNull UUID viewer) {
        return Collections.unmodifiableList(actions.getOrDefault(viewer, List.of()));
    }

    @NotNull
    public List<PacketAction> drain(@NotNull UUID viewer) {
        List<PacketAction> current = new ArrayList<>(actions.getOrDefault(viewer, List.of()));
        actions.remove(viewer);
        return current;
    }

    public void clear() {
        actions.clear();
        resetRenderer();
    }
}

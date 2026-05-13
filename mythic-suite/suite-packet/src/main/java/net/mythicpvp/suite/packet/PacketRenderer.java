package net.mythicpvp.suite.packet;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PacketRenderer {
    void render(@NotNull Player viewer, @NotNull PacketAction action);
}

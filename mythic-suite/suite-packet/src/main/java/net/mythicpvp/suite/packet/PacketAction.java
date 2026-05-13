package net.mythicpvp.suite.packet;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public sealed interface PacketAction permits PacketAction.TabHeaderFooter, PacketAction.ScoreboardState, PacketAction.NametagState, PacketAction.HologramState, PacketAction.EntityRefresh {

    @NotNull
    String id();

    record TabHeaderFooter(@NotNull String id, @NotNull Component header, @NotNull Component footer, @NotNull Map<UUID, Component> entries) implements PacketAction {}

    record ScoreboardState(@NotNull String id, @NotNull Component title, @NotNull List<Component> lines, @NotNull String fontKey) implements PacketAction {}

    record NametagState(@NotNull String id, @NotNull UUID target, @NotNull Component prefix, @NotNull Component suffix, int sortWeight, @Nullable String glowColor, @NotNull String displayName) implements PacketAction {}

    record HologramState(@NotNull String id, @NotNull List<Component> lines, boolean leaderboard, @NotNull String animationFrame) implements PacketAction {}

    record EntityRefresh(@NotNull String id, @NotNull UUID target, @NotNull String displayName, @Nullable String skinValue, @Nullable String skinSignature) implements PacketAction {}

    static void send(@NotNull Player viewer, @NotNull PacketAction action) {
        PacketSession.getInstance().send(viewer, action);
    }
}

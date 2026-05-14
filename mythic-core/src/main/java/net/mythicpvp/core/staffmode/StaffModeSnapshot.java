package net.mythicpvp.core.staffmode;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record StaffModeSnapshot(
        @NotNull ItemStack[] contents,
        @NotNull ItemStack[] armor,
        ItemStack offhand,
        @NotNull GameMode gameMode,
        boolean allowFlight,
        boolean flying
) {}

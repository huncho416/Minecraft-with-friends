package net.mythicpvp.core.staffmode;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public record StaffModeTool(
        int slot,
        @NotNull Material material,
        @NotNull String name,
        @NotNull String type
) {}

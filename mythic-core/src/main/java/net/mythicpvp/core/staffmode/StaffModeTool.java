package net.mythicpvp.core.staffmode;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * One configurable tool in the staff-mode hotbar palette.
 *
 * @param slot     hotbar slot 0..8 (out-of-range entries are skipped at load)
 * @param material Bukkit material name (resolved at apply time)
 * @param name     display name with Mythic hex parsing
 * @param type     identifier — {@link StaffModeTools} switches on this
 *                 to dispatch the right action when a player right-clicks.
 *                 Free-form so YAML can add new types without recompiling
 *                 the recognition table; unknown types render but do nothing.
 */
public record StaffModeTool(
        int slot,
        @NotNull Material material,
        @NotNull String name,
        @NotNull String type
) {}

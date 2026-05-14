package net.mythicpvp.core.staffmode;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Snapshot of a player's pre-staff-mode state. Captured at enable time,
 * restored at disable time (or at the next login if the server restarted
 * mid-staff-mode — see {@link SafeRestoreListener}).
 *
 * <p>Inventory + armor are copied by reference but {@link ItemStack} is
 * itself mutable; we trust no other code touches the array between
 * capture and restore. Bukkit's inventory APIs return cloned arrays so
 * the snapshot is structurally safe.
 *
 * @param contents      slots 0..35 (hotbar + main inventory)
 * @param armor         the 4 armor slots (boots, leggings, chestplate, helmet)
 * @param offhand       offhand slot
 * @param gameMode      the player's pre-staff gamemode
 * @param allowFlight   whether {@code allow-flight} was set
 * @param flying        whether the player was actively flying
 */
public record StaffModeSnapshot(
        @NotNull ItemStack[] contents,
        @NotNull ItemStack[] armor,
        ItemStack offhand,
        @NotNull GameMode gameMode,
        boolean allowFlight,
        boolean flying
) {}

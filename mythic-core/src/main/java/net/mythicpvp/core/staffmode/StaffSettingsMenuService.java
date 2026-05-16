package net.mythicpvp.core.staffmode;

import net.mythicpvp.core.staff.StaffChannel;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StaffSettingsMenuService {

    private final StaffSettings settings;

    public StaffSettingsMenuService(@NotNull StaffSettings settings) {
        this.settings = settings;
    }

    public void open(@NotNull Player viewer) {
        MythicMenu menu = MythicMenu.create(3, "&#F529BEStaff Settings");

        menu.slot(10, toggleItem(
                "&#FFEC8AForce Staff Mode on Join",
                "Lower-rank staff are placed in staff mode when they connect.",
                settings.forceStaffModeOnJoin()), event -> {
            settings.setForceStaffModeOnJoin(!settings.forceStaffModeOnJoin());
            open(viewer);
        });

        menu.slot(12, toggleItem(
                "&#FFEC8AForce Vanish on Join",
                "Lower-rank staff connect vanished to other players.",
                settings.forceVanishOnJoin()), event -> {
            settings.setForceVanishOnJoin(!settings.forceVanishOnJoin());
            open(viewer);
        });

        menu.slot(14, toggleItem(
                "&#FFEC8AForce Staff Chat on Join",
                "Lower-rank staff have a staff chat toggled on connection.",
                settings.forceStaffChatOnJoin()), event -> {
            settings.setForceStaffChatOnJoin(!settings.forceStaffChatOnJoin());
            open(viewer);
        });

        menu.slot(16, MythicItem.create(Material.NAME_TAG)
                .name("&#FFEC8AForced Staff Channel: " + settings.forcedChannel().tagColor()
                        + settings.forcedChannel().tag())
                .lore(
                        "&7Click to cycle which staff chat",
                        "&7channel is forced when the toggle above is on.",
                        "",
                        "&#9CFF9CClick to cycle.")
                .build(), event -> {
            settings.setForcedChannel(nextChannel(settings.forcedChannel()));
            open(viewer);
        });

        menu.slot(22, MythicItem.create(Material.PAPER)
                .name("&#D2D8E0Bypass permission")
                .lore(
                        "&7Players with &f" + StaffSettings.BYPASS_PERMISSION,
                        "&7are exempt from the toggles above.",
                        "&7Grant this to senior staff who manage their own state.")
                .build(), event -> {
        });

        menu.open(viewer);
    }

    @NotNull
    private static org.bukkit.inventory.ItemStack toggleItem(@NotNull String displayName,
                                                             @NotNull String description,
                                                             boolean enabled) {
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String state = enabled ? "&aENABLED" : "&8DISABLED";
        return MythicItem.create(mat)
                .name(displayName)
                .lore(
                        "&7" + description,
                        "",
                        "&7State: " + state,
                        "",
                        "&#9CFF9CClick to toggle.")
                .build();
    }

    @NotNull
    private static StaffChannel nextChannel(@NotNull StaffChannel current) {
        StaffChannel[] all = StaffChannel.values();
        for (int i = 0; i < all.length; i++) {
            if (all[i] == current) {
                return all[(i + 1) % all.length];
            }
        }
        return StaffChannel.STAFF;
    }

}

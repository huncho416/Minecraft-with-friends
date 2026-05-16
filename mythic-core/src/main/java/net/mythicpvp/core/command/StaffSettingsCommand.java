package net.mythicpvp.core.command;

import net.mythicpvp.core.staffmode.StaffSettingsMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("staffsettings")
@CommandPermission("mythic.core.staffsettings")
public final class StaffSettingsCommand extends MythicCommand {

    private final StaffSettingsMenuService menu;

    public StaffSettingsCommand(@NotNull StaffSettingsMenuService menu) {
        this.menu = menu;
    }

    @Default
    public void execute(@NotNull Player player) {
        menu.open(player);
    }
}

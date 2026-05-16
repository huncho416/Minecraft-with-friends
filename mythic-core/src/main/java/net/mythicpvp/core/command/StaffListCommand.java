package net.mythicpvp.core.command;

import net.mythicpvp.core.staff.StaffListMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("stafflist")
@CommandPermission("mythic.core.stafflist")
public final class StaffListCommand extends MythicCommand {

    private final StaffListMenuService menu;

    public StaffListCommand(@NotNull StaffListMenuService menu) {
        this.menu = menu;
    }

    @Default
    public void execute(@NotNull Player player) {
        menu.open(player);
    }
}

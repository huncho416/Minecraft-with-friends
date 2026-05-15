package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.staffmode.StaffModeService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("staffmode")
@CommandPermission("mythic.core.staffmode")
public final class StaffModeCommand extends MythicCommand {

    private final StaffModeService staffMode;
    private final CoreMessages messages;

    public StaffModeCommand(@NotNull StaffModeService staffMode, @NotNull CoreMessages messages) {
        this.staffMode = staffMode;
        this.messages = messages;
    }

    @Default
    public void execute(@NotNull Player player) {
        boolean nowEnabled = staffMode.toggle(player);
        player.sendMessage(messages.component(
                nowEnabled ? "messages.staff-mode.enabled" : "messages.staff-mode.disabled",
                nowEnabled
                        ? "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CStaff mode enabled."
                        : "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CStaff mode disabled."));
    }
}

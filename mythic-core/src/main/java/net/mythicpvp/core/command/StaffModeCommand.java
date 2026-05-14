package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.staffmode.StaffModeService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /staffmode} (alias {@code /sm}) — toggle the caller into staff
 * mode. Player-only because staff mode mutates inventory + flight +
 * vanish, all of which require an actual player.
 *
 * <p>Permission: {@code mythic.core.staffmode}.
 */
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
                        ? "&#FF00F8Staff &8» &#FFFFFFStaff mode enabled."
                        : "&#FF00F8Staff &8» &#FFFFFFStaff mode disabled."));
    }
}

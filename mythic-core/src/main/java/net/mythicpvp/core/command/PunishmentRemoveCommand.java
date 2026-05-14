package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("punishmentremove")
@CommandPermission("mythic.core.punish.template.remove")
public final class PunishmentRemoveCommand extends MythicCommand {

    private final PunishmentService punishmentService;

    public PunishmentRemoveCommand(@NotNull PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Default
    @Complete({"punishment-templates"})
    public void execute(@NotNull CommandSender sender, @NotNull String[] titleParts) {
        String title = String.join(" ", titleParts).trim();
        sender.sendMessage(punishmentService.removeTemplate(title) ? "Removed punishment template " + title + "." : "Unknown punishment template.");
    }
}

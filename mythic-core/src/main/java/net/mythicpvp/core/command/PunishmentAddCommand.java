package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("punishmentadd")
@CommandPermission("mythic.core.punish.template.add")
public final class PunishmentAddCommand extends MythicCommand {

    private final PunishmentService punishmentService;

    public PunishmentAddCommand(@NotNull PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Default
    @Complete({"punishment-categories", "punishment-durations"})
    public void execute(@NotNull CommandSender sender, @NotNull String category, @NotNull String duration, @NotNull String[] titleAndInformation) {
        String payload = String.join(" ", titleAndInformation).trim();
        if (payload.isBlank()) {
            sender.sendMessage("Missing punishment title.");
            return;
        }
        String[] parts = payload.split("\\|", 2);
        String title = parts[0].trim();
        String information = parts.length > 1 ? parts[1].trim() : "";
        punishmentService.addTemplate(PunishmentCategory.parse(category), duration, title, information);
        sender.sendMessage("Added punishment template " + title + ".");
    }
}

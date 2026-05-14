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
        // Two accepted shapes for title + information:
        //   1. Pipe-delimited (legacy):   /punishmentadd MUTE 1d Chat Offense | first chat offense
        //   2. Quoted (new):              /punishmentadd MUTE 1d "Chat Offense" first chat offense
        // Try quoted first because it's unambiguous; fall back to pipe.
        String title;
        String information;
        if (payload.startsWith("\"")) {
            java.util.List<String> tokens = QuotedArgs.parse(payload);
            if (tokens.isEmpty()) {
                sender.sendMessage("Missing punishment title.");
                return;
            }
            title = tokens.get(0);
            information = tokens.size() > 1
                    ? String.join(" ", tokens.subList(1, tokens.size()))
                    : "";
        } else {
            String[] parts = payload.split("\\|", 2);
            title = parts[0].trim();
            information = parts.length > 1 ? parts[1].trim() : "";
        }
        punishmentService.addTemplate(PunishmentCategory.parse(category), duration, title, information);
        sender.sendMessage("Added punishment template " + title + ".");
    }
}

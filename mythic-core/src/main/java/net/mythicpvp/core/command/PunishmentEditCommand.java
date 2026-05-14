package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("punishmentedit")
@CommandPermission("mythic.core.punish.template.edit")
public final class PunishmentEditCommand extends MythicCommand {

    private final PunishmentService punishmentService;

    public PunishmentEditCommand(@NotNull PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Default
    @Complete({"punishment-templates"})
    public void execute(@NotNull Player player, @NotNull String[] titleParts) {
        PunishmentTemplate template = punishmentService.template(String.join(" ", titleParts));
        if (template == null) {
            player.sendMessage("Unknown punishment template.");
            return;
        }
        MythicMenu.create(3, "&#F529BEEdit: " + template.title())
                .slot(10, MythicItem.create(template.category().material()).name("&#F529BE" + template.title()).lore(List.of("&7Category: &f" + template.category().name(), "&7Duration: &f" + template.duration(), "&7Info: &f" + template.information())).build())
                .slot(16, MythicItem.create(Material.BARRIER).name("&#F529BEClose").build(), event -> player.closeInventory())
                .open(player);
    }

    @Subcommand("set")
    @Complete({"punishment-templates", "punishment-categories", "punishment-durations"})
    public void set(@NotNull CommandSender sender, @NotNull String oldTitle, @NotNull String category, @NotNull String duration, @NotNull String[] titleAndInformation) {
        String payload = String.join(" ", titleAndInformation).trim();
        if (payload.isBlank()) {
            sender.sendMessage("Missing punishment title.");
            return;
        }
        String[] parts = payload.split("\\|", 2);
        String title = parts[0].trim();
        String information = parts.length > 1 ? parts[1].trim() : "";
        boolean updated = punishmentService.editTemplate(oldTitle, PunishmentCategory.parse(category), duration, title, information);
        sender.sendMessage(updated ? "Updated punishment template " + title + "." : "Unknown punishment template.");
    }
}

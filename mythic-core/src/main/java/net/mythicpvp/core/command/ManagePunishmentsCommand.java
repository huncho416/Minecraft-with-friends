package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.ManagePunishmentsMenuService;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandAlias("managepunishments")
@CommandPermission("mythic.core.punish.manage")
public final class ManagePunishmentsCommand extends MythicCommand {

    private final ManagePunishmentsMenuService menu;
    private final PunishmentService punishmentService;

    public ManagePunishmentsCommand(@NotNull ManagePunishmentsMenuService menu,
                                    @NotNull PunishmentService punishmentService) {
        this.menu = menu;
        this.punishmentService = punishmentService;
    }

    @Default
    public void execute(@NotNull Player player) {
        menu.openOverview(player);
    }

    @Subcommand("clear")
    public void clear(@NotNull Player player, String categoryArg) {
        if (categoryArg == null || categoryArg.isBlank()) {
            sendUsage(player);
            return;
        }
        PunishmentType type = parseType(categoryArg);
        if (type == null) {
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8AUnknown category: &#FFFFFF" + categoryArg
                            + "&#FF8A8A. Valid: &#FFFFFF" + validCategoryList()));
            return;
        }
        int removed = punishmentService.clearByType(type, player.getUniqueId());
        player.sendMessage(MythicHex.colorize(
                "&#FF8A8ACleared &#FFFFFF" + removed + " &#FF8A8A"
                        + type.name().toLowerCase(Locale.ROOT) + " record(s)."));
    }

    private void sendUsage(@NotNull Player player) {
        player.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/managepunishments clear <category>"));
        player.sendMessage(MythicHex.colorize("&7Categories: &f" + validCategoryList()));
    }

    private static PunishmentType parseType(@NotNull String input) {
        String norm = input.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (PunishmentType type : PunishmentType.values()) {
            if (type.name().equals(norm)) {
                return type;
            }
        }
        return null;
    }

    @NotNull
    private static String validCategoryList() {
        return Arrays.stream(PunishmentType.values())
                .map(t -> t.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}

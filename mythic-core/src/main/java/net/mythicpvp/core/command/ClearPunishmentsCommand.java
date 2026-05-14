package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("clearpunishments|clearhistory")
@CommandPermission("mythic.core.punish.clearhistory")
public final class ClearPunishmentsCommand extends MythicCommand {

    private final PunishmentService punishmentService;

    public ClearPunishmentsCommand(@NotNull PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        int removed = punishmentService.clearHistory(target.getUniqueId());
        sender.sendMessage("Cleared " + removed + " punishment records for " + targetName + ".");
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

@CommandAlias("unmute")
@CommandPermission("mythic.core.punish.mute")
public final class UnmuteCommand extends MythicCommand {

    private final PunishmentService punishments;

    public UnmuteCommand(@NotNull PunishmentService punishments) {
        this.punishments = punishments;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/unmute <player> [-s]"));
            return;
        }
        boolean silent = false;
        String targetName = null;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (targetName == null) {
                targetName = arg;
            }
        }
        if (targetName == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/unmute <player> [-s]"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID staffUuid = sender instanceof Player player ? player.getUniqueId() : PunishmentService.SYSTEM_STAFF;
        int count = punishments.pardonActive(target.getUniqueId(),
                Set.of(PunishmentType.MUTE, PunishmentType.TEMP_MUTE),
                staffUuid,
                sender.getName(),
                "Unmuted",
                silent);
        sender.sendMessage(MythicHex.colorize(count > 0
                ? "&#9CFF9CUnmuted &f" + targetName + "&#9CFF9C" + (silent ? " &7(silent)" : "") + "."
                : "&#FF8A8ANo active mute found for &f" + targetName + "&#FF8A8A."));
    }
}

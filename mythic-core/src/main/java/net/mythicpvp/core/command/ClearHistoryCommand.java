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

import java.util.EnumSet;
import java.util.UUID;

@CommandAlias("clearhistory")
@CommandPermission("mythic.core.punish.clearhistory")
public final class ClearHistoryCommand extends MythicCommand {

    private final PunishmentService punishmentService;

    public ClearHistoryCommand(@NotNull PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID staffUuid = sender instanceof Player p ? p.getUniqueId() : PunishmentService.SYSTEM_STAFF;
        int pardoned = punishmentService.pardonActive(
                target.getUniqueId(),
                EnumSet.of(PunishmentType.BAN, PunishmentType.TEMP_BAN, PunishmentType.BLACKLIST,
                        PunishmentType.MUTE, PunishmentType.TEMP_MUTE, PunishmentType.WARN, PunishmentType.KICK),
                staffUuid,
                "Cleared by " + sender.getName());
        int removed = punishmentService.clearHistory(target.getUniqueId(), staffUuid);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CWiped &f" + removed + " &#9CFF9Crecord(s) (&f" + pardoned
                        + " &#9CFF9Cstill-active first) for &f" + targetName + "&#9CFF9C."));
    }
}

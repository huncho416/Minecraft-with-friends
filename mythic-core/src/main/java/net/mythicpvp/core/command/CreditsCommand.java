package net.mythicpvp.core.command;

import net.mythicpvp.core.credit.CreditService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("credits")
@CommandPermission("mythic.core.credits")
public final class CreditsCommand extends MythicCommand {

    private final CreditService creditService;

    public CreditsCommand(@NotNull CreditService creditService) {
        this.creditService = creditService;
    }

    @Default
    public void balance(@NotNull Player player) {
        long bal = creditService.getBalance(player.getUniqueId());
        player.sendMessage(MythicHex.colorize("&#FFD700Credits: &f" + bal));
    }

    @Subcommand("give")
    @CommandPermission("mythic.core.credits.admin")
    @Complete({"players", ""})
    public void give(@NotNull CommandSender sender, @NotNull String target, long amount) {
        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8APlayer not found."));
            return;
        }
        creditService.give(targetPlayer.getUniqueId(), amount);
        sender.sendMessage(MythicHex.colorize("&#9CFF9CGave &f" + amount + " &#9CFF9Ccredits to &f" + target + "&#9CFF9C."));
    }

    @Subcommand("set")
    @CommandPermission("mythic.core.credits.admin")
    @Complete({"players", ""})
    public void set(@NotNull CommandSender sender, @NotNull String target, long amount) {
        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8APlayer not found."));
            return;
        }
        creditService.set(targetPlayer.getUniqueId(), amount);
        sender.sendMessage(MythicHex.colorize("&#9CFF9CSet &f" + target + "&#9CFF9C's credits to &f" + amount + "&#9CFF9C."));
    }

    @Subcommand("check")
    @CommandPermission("mythic.core.credits.admin")
    @Complete({"players"})
    public void check(@NotNull CommandSender sender, @NotNull String target) {
        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8APlayer not found."));
            return;
        }
        long bal = creditService.getBalance(targetPlayer.getUniqueId());
        sender.sendMessage(MythicHex.colorize("&#FFD700" + target + "'s credits: &f" + bal));
    }
}

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
        if (player.hasPermission("mythic.core.credits.admin")) {
            player.sendMessage(MythicHex.colorize("&7Type &#FFFFFF/credits help &7for admin subcommands."));
        }
    }

    @Subcommand("help")
    @CommandPermission("mythic.core.credits.admin")
    public void help(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#F529BECredits Admin"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/credits &7- show your own balance"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/credits give <player> <amount> &7- add credits to a player"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/credits set <player> <amount> &7- overwrite a player's balance"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/credits check <player> &7- view another player's balance"));
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

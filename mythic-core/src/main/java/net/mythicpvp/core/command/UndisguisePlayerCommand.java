package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("undisguiseplayer|udplayer")
@CommandPermission("mythic.core.disguise")
public final class UndisguisePlayerCommand extends MythicCommand {

    private final DisguiseApplier applier;

    public UndisguisePlayerCommand(@NotNull DisguiseApplier applier) {
        this.applier = applier;
    }

    @Default
    public void execute(@NotNull CommandSender sender, @NotNull String[] words) {
        if (words.length < 1) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/undisguiseplayer <target>"));
            return;
        }
        boolean canTargetOthers = sender.hasPermission("mythic.core.disguise.others")
                || sender.hasPermission("mythic.core.disguise.*");
        if (!canTargetOthers && !(sender instanceof Player p && p.getName().equalsIgnoreCase(words[0]))) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou need &#FFFFFFmythic.core.disguise.others&#FF8A8A to undisguise other players."));
            return;
        }
        Player target = Bukkit.getPlayerExact(words[0]);
        if (target == null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8APlayer &#FFFFFF" + words[0] + "&#FF8A8A is not online."));
            return;
        }
        if (!applier.isDisguised(target.getUniqueId())) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8A" + target.getName() + " is not disguised."));
            return;
        }
        applier.undisguise(target);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CCleared disguise from &#FFFFFF" + target.getName() + "&#9CFF9C."));
        if (!sender.equals(target)) {
            target.sendMessage(MythicHex.colorize("&#9CFF9CYour disguise was cleared."));
        }
    }
}

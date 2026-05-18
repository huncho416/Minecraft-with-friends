package net.mythicpvp.core.command;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("disguisehelp|dhelp")
@CommandPermission("mythic.core.disguise")
public final class DisguiseHelpCommand extends MythicCommand {

    @Default
    public void execute(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#FF00F8Disguise commands:"));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguise &8» &#D2D8E0Open the disguise menu (rank/skin/name)."));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguise <name> &8» &#D2D8E0Quick name-only disguise."));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/undisguise &8» &#D2D8E0Clear your own disguise."));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguiseclone <name> &8» &#D2D8E0Clone a Minecraft account's name + skin."));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/copydisguise <player> &8» &#D2D8E0Copy another disguised player's disguise."));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguisemodify name=… skin=… rank=… &8» &#D2D8E0Tweak the active disguise."));
        sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguiseviewself &8» &#D2D8E0Toggle seeing through other players' disguises."));
        if (sender.hasPermission("mythic.core.disguise.others")
                || sender.hasPermission("mythic.core.disguise.*")) {
            sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguiseplayer <target> <name> [skin] &8» &#D2D8E0Disguise another player."));
            sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/undisguiseplayer <target> &8» &#D2D8E0Clear another player's disguise."));
        }
        if (sender.hasPermission("mythic.core.disguise.radius")) {
            sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/disguiseradius <radius> <name> &8» &#D2D8E0Disguise all nearby players."));
            sender.sendMessage(MythicHex.colorize("&7- &#FFFFFF/undisguiseradius <radius> &8» &#D2D8E0Clear nearby disguises."));
        }
        sender.sendMessage(MythicHex.colorize(
                "&8Mob/misc disguise types arrive in a follow-up batch."));
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("copydisguise|cdisguise")
@CommandPermission("mythic.core.disguise")
public final class CopyDisguiseCommand extends MythicCommand {

    private final DisguiseApplier applier;

    public CopyDisguiseCommand(@NotNull DisguiseApplier applier) {
        this.applier = applier;
    }

    @Default
    public void execute(@NotNull Player sender, @NotNull String[] words) {
        if (words.length < 1) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/copydisguise <player>"));
            return;
        }
        Player source = Bukkit.getPlayerExact(words[0]);
        if (source == null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8APlayer &#FFFFFF" + words[0] + "&#FF8A8A is not online."));
            return;
        }
        DisguiseManager.DisguiseData data = DisguiseManager.getInstance().getDisguise(source.getUniqueId());
        if (data == null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8A" + source.getName() + " is not disguised."));
            return;
        }
        String skinValue = data.skinValue();
        String skinSignature = data.skinSignature();
        applier.apply(sender, data.displayName(), skinValue, skinSignature, data.rankOverride());
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CCopied disguise from &#FFFFFF" + source.getName() + "&#9CFF9C: now &#FFFFFF"
                        + data.displayName() + "&#9CFF9C."));
    }
}

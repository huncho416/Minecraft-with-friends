package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("undisguiseradius|udradius")
@CommandPermission("mythic.core.disguise.radius")
public final class UndisguiseRadiusCommand extends MythicCommand {

    private final DisguiseApplier applier;

    public UndisguiseRadiusCommand(@NotNull DisguiseApplier applier) {
        this.applier = applier;
    }

    @Default
    public void execute(@NotNull Player sender, @NotNull String[] words) {
        if (words.length < 1) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/undisguiseradius <radius>"));
            return;
        }
        int radius;
        try {
            radius = Math.min(Math.max(1, Integer.parseInt(words[0])), 200);
        } catch (NumberFormatException ex) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ARadius must be a number 1-200."));
            return;
        }
        List<Player> targets = sender.getWorld().getNearbyEntities(sender.getLocation(), radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> applier.isDisguised(p.getUniqueId()))
                .toList();
        if (targets.isEmpty()) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8ANo disguised players within " + radius + " blocks."));
            return;
        }
        for (Player target : targets) {
            applier.undisguise(target);
        }
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CCleared disguises from &#FFFFFF" + targets.size() + " &#9CFF9Cplayer(s)."));
    }
}

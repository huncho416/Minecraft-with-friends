package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
import net.mythicpvp.core.disguise.DisguiseTypeRegistry;
import net.mythicpvp.core.disguise.MojangSkinService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("disguiseradius|dradius")
@CommandPermission("mythic.core.disguise.radius")
public final class DisguiseRadiusCommand extends MythicCommand {

    private final DisguiseApplier applier;
    private final DisguiseTypeRegistry registry;
    private final MojangSkinService skins;
    private final JavaPlugin plugin;

    public DisguiseRadiusCommand(@NotNull DisguiseApplier applier,
                                 @NotNull DisguiseTypeRegistry registry,
                                 @NotNull MojangSkinService skins,
                                 @NotNull JavaPlugin plugin) {
        this.applier = applier;
        this.registry = registry;
        this.skins = skins;
        this.plugin = plugin;
    }

    @Default
    public void execute(@NotNull Player sender, @NotNull String[] words) {
        if (words.length < 2) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AUsage: &#FFFFFF/disguiseradius <radius> <name>"));
            return;
        }
        int radius;
        try {
            radius = Math.min(Math.max(1, Integer.parseInt(words[0])), 200);
        } catch (NumberFormatException ex) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ARadius must be a number 1-200."));
            return;
        }
        if (!registry.canUse(sender, "player")) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou are not allowed to use player disguises."));
            return;
        }
        String displayName = words[1];
        List<Player> targets = sender.getWorld().getNearbyEntities(sender.getLocation(), radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList();
        if (targets.isEmpty()) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ANo players within " + radius + " blocks."));
            return;
        }
        sender.sendMessage(MythicHex.colorize(
                "&#D2D8E0Fetching skin and disguising &#FFFFFF" + targets.size() + " &#D2D8E0player(s)…"));
        skins.lookup(displayName).thenAccept(result -> MythicScheduler.runSync(plugin, () -> {
            String value = result.skinValue();
            String signature = result.skinSignature();
            for (Player target : targets) {
                applier.apply(target, displayName, value, signature, null);
            }
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CDisguised &#FFFFFF" + targets.size() + " &#9CFF9Cplayer(s) as &#FFFFFF"
                            + displayName + "&#9CFF9C."));
        }));
    }
}

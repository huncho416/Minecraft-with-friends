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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@CommandAlias("disguiseplayer|dplayer")
@CommandPermission("mythic.core.disguise")
public final class DisguisePlayerCommand extends MythicCommand {

    private final DisguiseApplier applier;
    private final DisguiseTypeRegistry registry;
    private final MojangSkinService skins;
    private final JavaPlugin plugin;

    public DisguisePlayerCommand(@NotNull DisguiseApplier applier,
                                 @NotNull DisguiseTypeRegistry registry,
                                 @NotNull MojangSkinService skins,
                                 @NotNull JavaPlugin plugin) {
        this.applier = applier;
        this.registry = registry;
        this.skins = skins;
        this.plugin = plugin;
    }

    @Default
    public void execute(@NotNull CommandSender sender, @NotNull String[] words) {
        if (words.length < 2) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AUsage: &#FFFFFF/disguiseplayer <target> <name> [skin-source]"));
            return;
        }
        boolean canTargetOthers = sender.hasPermission("mythic.core.disguise.others")
                || sender.hasPermission("mythic.core.disguise.*");
        if (!canTargetOthers && !(sender instanceof Player p && p.getName().equalsIgnoreCase(words[0]))) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou need &#FFFFFFmythic.core.disguise.others&#FF8A8A to disguise other players."));
            return;
        }
        if (!registry.canUse(sender, "player")) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou are not allowed to use player disguises."));
            return;
        }
        Player target = Bukkit.getPlayerExact(words[0]);
        if (target == null) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8APlayer &#FFFFFF" + words[0] + "&#FF8A8A is not online."));
            return;
        }
        String displayName = words[1];
        String skinSource = words.length >= 3 ? words[2] : displayName;
        sender.sendMessage(MythicHex.colorize(
                "&#D2D8E0Fetching skin for &#FFFFFF" + skinSource + "&#D2D8E0…"));
        skins.lookup(skinSource).thenAccept(result -> MythicScheduler.runSync(plugin, () -> {
            String value = result.skinValue();
            String signature = result.skinSignature();
            applier.apply(target, displayName, value, signature, null);
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CDisguised &#FFFFFF" + target.getName() + "&#9CFF9C as &#FFFFFF"
                            + displayName + (value == null ? " &#D2D8E0(no skin found)" : " &#D2D8E0(with skin)")
                            + "&#9CFF9C."));
            if (!sender.equals(target)) {
                target.sendMessage(MythicHex.colorize(
                        "&#9CFF9CYou have been disguised as &#FFFFFF" + displayName + "&#9CFF9C."));
            }
        }));
    }
}

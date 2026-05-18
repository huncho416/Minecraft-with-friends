package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
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

@CommandAlias("disguiseclone|dclone")
@CommandPermission("mythic.core.disguise")
public final class DisguiseCloneCommand extends MythicCommand {

    private final DisguiseApplier applier;
    private final MojangSkinService skins;
    private final JavaPlugin plugin;

    public DisguiseCloneCommand(@NotNull DisguiseApplier applier,
                                @NotNull MojangSkinService skins,
                                @NotNull JavaPlugin plugin) {
        this.applier = applier;
        this.skins = skins;
        this.plugin = plugin;
    }

    @Default
    public void execute(@NotNull Player sender, @NotNull String[] words) {
        if (words.length < 1) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AUsage: &#FFFFFF/disguiseclone <minecraft-name>"));
            return;
        }
        String name = words[0];
        sender.sendMessage(MythicHex.colorize(
                "&#D2D8E0Cloning skin of &#FFFFFF" + name + "&#D2D8E0…"));
        skins.lookup(name).thenAccept(result -> MythicScheduler.runSync(plugin, () -> {
            if (!result.success()) {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8AUnknown account &#FFFFFF" + name + "&#FF8A8A."));
                return;
            }
            applier.apply(sender, result.resolvedName(), result.skinValue(), result.skinSignature(), null);
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CCloned &#FFFFFF" + result.resolvedName() + "&#9CFF9C."));
        }));
    }
}

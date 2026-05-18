package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("disguise|d")
@CommandPermission("mythic.core.disguise")
public final class DisguiseCommand extends MythicCommand {

    private final DisguiseMenuService menuService;
    private final DisguiseApplier applier;

    public DisguiseCommand(@NotNull DisguiseMenuService menuService, @NotNull DisguiseApplier applier) {
        this.menuService = menuService;
        this.applier = applier;
    }

    @Default
    public void execute(@NotNull Player player, @NotNull String[] words) {
        if (words.length == 0) {
            menuService.openMain(player);
            return;
        }
        String name = words[0];
        applier.apply(player, name, null, null, null);
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CDisguised as &#FFFFFF" + name + "&#9CFF9C."));
    }

    @CommandAlias("undisguise|ud")
    @CommandPermission("mythic.core.disguise")
    public static final class Undisguise extends MythicCommand {

        private final DisguiseApplier applier;

        public Undisguise(@NotNull DisguiseApplier applier) {
            this.applier = applier;
        }

        @Default
        public void execute(@NotNull Player player) {
            applier.undisguise(player);
            player.sendMessage(MythicHex.colorize("&#9CFF9CDisguise cleared."));
        }
    }
}

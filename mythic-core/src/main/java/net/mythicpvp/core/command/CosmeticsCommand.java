package net.mythicpvp.core.command;

import net.mythicpvp.core.cosmetic.CosmeticMenuService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("cosmetics")
public final class CosmeticsCommand extends MythicCommand {

    private final CosmeticMenuService menuService;

    public CosmeticsCommand(@NotNull CosmeticMenuService menuService) {
        this.menuService = menuService;
    }

    @Default
    public void execute(@NotNull Player player) {
        menuService.openMain(player);
    }
}

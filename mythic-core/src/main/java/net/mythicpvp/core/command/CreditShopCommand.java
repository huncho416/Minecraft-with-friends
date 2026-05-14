package net.mythicpvp.core.command;

import net.mythicpvp.core.credit.CreditShopService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("creditshop")
@CommandPermission("mythic.core.creditshop")
public final class CreditShopCommand extends MythicCommand {

    private final CreditShopService shopService;

    public CreditShopCommand(@NotNull CreditShopService shopService) {
        this.shopService = shopService;
    }

    @Default
    public void execute(@NotNull Player player) {
        shopService.openMain(player);
    }
}

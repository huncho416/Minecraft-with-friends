package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("grant")
@CommandPermission("mythic.core.grant.menu")
public final class GrantCommand extends MythicCommand {

    private final GrantService grantService;
    private final RankService rankService;

    public GrantCommand(@NotNull GrantService grantService, @NotNull RankService rankService) {
        this.grantService = grantService;
        this.rankService = rankService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player player, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        PaginatedMenu menu = PaginatedMenu.create(6, "&#FF00F8Grant: " + targetName);
        for (var rank : rankService.all()) {
            menu.addItem(MythicItem.create(rank.dye())
                    .name(rank.color() + rank.name())
                    .lore(List.of(
                            "&7Staff Rank: &f" + yesNo(rank.staff()),
                            "&7Purchaseable: &f" + yesNo(rank.donator()),
                            "&7Prefix: &f" + rank.prefix(),
                            "&7Parent: &f" + (rank.parent().isBlank() ? "None" : rank.parent()),
                            "&7Weight: &f" + rank.weight(),
                            "&7Permissions: &f" + rank.permissions().size(),
                            "&#FF00F8Click to grant permanent"
                    ))
                    .build(), event -> {
                        grantService.grant(target.getUniqueId(), targetName, rank.id(), GrantDuration.parse("permanent"), player.getUniqueId(), player.getName(), "Menu Grant");
                        player.closeInventory();
                        player.sendMessage("Granted " + rank.id() + " to " + targetName + ".");
                    });
        }
        menu.open(player);
    }

    @NotNull
    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}

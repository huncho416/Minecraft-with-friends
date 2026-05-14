package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@CommandAlias("grants")
@CommandPermission("mythic.core.grant.history")
public final class GrantsCommand extends MythicCommand {

    private final GrantService grantService;
    private final RankService rankService;

    public GrantsCommand(@NotNull GrantService grantService, @NotNull RankService rankService) {
        this.grantService = grantService;
        this.rankService = rankService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player player, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BEGrants: " + targetName);
        for (RankGrant grant : grantService.history(target.getUniqueId())) {
            Material material = grant.active() && rankService.get(grant.rankId()) != null ? rankService.get(grant.rankId()).dye() : Material.GRAY_DYE;
            menu.addItem(MythicItem.create(material)
                    .name("&#F529BE" + grant.rankId())
                    .lore(lore(grant))
                    .build(), event -> {
                        if (event.isLeftClick() && grant.active()) {
                            grantService.deactivate(grant.id());
                            execute(player, targetName);
                        } else if (event.isRightClick() && !grant.active()) {
                            grantService.removeInactive(grant.id());
                            execute(player, targetName);
                        }
                    });
        }
        menu.open(player);
    }

    @NotNull
    private static List<String> lore(@NotNull RankGrant grant) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Duration: &f" + (grant.permanent() ? "Permanent" : Duration.ofMillis(Math.max(0, grant.expiresAtMillis() - System.currentTimeMillis())).toDays() + "d remaining"));
        lore.add("&7Executor: &f" + grant.executorName());
        lore.add("&7Reason: &f" + grant.reason());
        lore.add("&7State: &f" + (grant.active() ? "Active" : "Inactive"));
        lore.add(grant.active() ? "&#F529BELeft click to deactivate" : "&#F529BERight click to remove");
        return lore;
    }
}

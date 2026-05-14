package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("rankeditor")
@CommandPermission("mythic.core.rank.editor")
public final class RankEditorCommand extends MythicCommand {

    private final RankService rankService;

    public RankEditorCommand(@NotNull RankService rankService) {
        this.rankService = rankService;
    }

    @Default
    @Complete({"ranks"})
    public void execute(@NotNull Player player, @NotNull String rankId) {
        var rank = rankService.get(rankId);
        if (rank == null) {
            player.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu.create(3, "&#FF00F8Rank Editor: " + rank.id())
                .slot(10, MythicItem.create(rank.dye()).name(rank.color() + rank.name()).lore(List.of("&7Weight: &f" + rank.weight(), "&7Staff: &f" + rank.staff(), "&7Donator: &f" + rank.donator())).build())
                .slot(12, MythicItem.create(Material.NAME_TAG).name("&#FF00F8Display Formats").lore(List.of("&7Chat: &f" + rank.chatPrefix(), "&7Tab: &f" + rank.tabPrefix(), "&7Nametag: &f" + rank.nametagPrefix())).build())
                .slot(14, MythicItem.create(Material.BOOK).name("&#FF00F8Permissions").lore(rank.permissions()).build())
                .slot(16, MythicItem.create(Material.BARRIER).name("&#FF00F8Close").build(), event -> player.closeInventory())
                .open(player);
    }
}

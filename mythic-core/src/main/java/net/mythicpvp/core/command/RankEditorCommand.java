package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.RankEditorMenuService;
import net.mythicpvp.core.rank.RankMenuText;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandAlias("rankeditor")
@CommandPermission("mythic.core.rank.editor")
public final class RankEditorCommand extends MythicCommand {

    private final RankService rankService;
    private final RankMenuText text;
    @Nullable
    private final RankEditorMenuService editorMenu;

    public RankEditorCommand(@NotNull RankService rankService) {
        this(rankService, RankMenuText.DEFAULTS, null);
    }

    public RankEditorCommand(@NotNull RankService rankService, @NotNull RankMenuText text) {
        this(rankService, text, null);
    }

    public RankEditorCommand(@NotNull RankService rankService, @NotNull RankMenuText text,
                             @Nullable RankEditorMenuService editorMenu) {
        this.rankService = rankService;
        this.text = text;
        this.editorMenu = editorMenu;
    }

    @Default
    @Complete({"ranks"})
    public void execute(@NotNull Player player, @NotNull String[] args) {
        if (args.length == 0) {
            if (editorMenu != null) {
                editorMenu.openRankList(player);
                return;
            }
            player.sendMessage("Usage: /rankeditor <rank>");
            return;
        }
        String rankId = args[0];
        var rank = rankService.get(rankId);
        if (rank == null) {
            player.sendMessage("Unknown rank.");
            return;
        }

        if (editorMenu != null) {
            editorMenu.openOverview(player, rank.id());
            return;
        }
        MythicMenu.create(3, text.editorTitle(rank.id()))
                .slot(10, MythicItem.create(rank.dye()).name(rank.color() + rank.name()).lore(List.of("&7Weight: &f" + rank.weight(), "&7Staff: &f" + rank.staff(), "&7Donator: &f" + rank.donator())).build())
                .slot(12, MythicItem.create(Material.NAME_TAG).name(text.editorDisplayFormats()).lore(List.of("&7Chat: &f" + rank.chatPrefix(), "&7Tab: &f" + rank.tabPrefix(), "&7Nametag: &f" + rank.nametagPrefix())).build())
                .slot(14, MythicItem.create(Material.BOOK).name(text.editorPermissions()).lore(rank.permissions()).build())
                .slot(16, MythicItem.create(Material.BARRIER).name(text.editorClose()).build(), event -> player.closeInventory())
                .open(player);
    }

    @Subcommand("set")
    @Complete({"ranks", "rank-fields"})
    public void set(@NotNull Player player, @NotNull String rankId, @NotNull String field, @NotNull String[] valueParts) {
        if (valueParts.length == 0) {
            player.sendMessage("Missing value.");
            return;
        }
        boolean updated = rankService.setField(rankId, field, String.join(" ", valueParts));
        player.sendMessage(updated ? "Updated " + rankId + "." : "Unable to update rank.");
    }

    @Subcommand("addperm")
    @Complete({"ranks"})
    public void addPermission(@NotNull Player player, @NotNull String rankId, @NotNull String permission) {
        player.sendMessage(rankService.addPermission(rankId, permission) ? "Permission added." : "Unable to add permission.");
    }

    @Subcommand("removeperm")
    @Complete({"ranks"})
    public void removePermission(@NotNull Player player, @NotNull String rankId, @NotNull String permission) {
        player.sendMessage(rankService.removePermission(rankId, permission) ? "Permission removed." : "Unable to remove permission.");
    }
}

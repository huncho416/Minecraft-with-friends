package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandAlias("rank")
@CommandPermission("mythic.core.rank.admin")
public final class RankCommand extends MythicCommand {

    private final RankService rankService;

    public RankCommand(@NotNull RankService rankService) {
        this.rankService = rankService;
    }

    @Default
    public void usage(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#F529BERank Commands"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank list &7- list every rank"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank info <rank> &7- show a rank's full configuration"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank create <id> [name] &7- create a new rank (defaults: weight=1000, color=#D2D8E0)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank delete <id> &7- remove a rank (alias: remove)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank set <id> <field> <value...> &7- set a field (name, color, prefix, weight, staff, donator, parent, chat-prefix, tab-prefix, nametag-prefix, dye)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank addperm <id> <permission> &7- grant a permission node to the rank"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank removeperm <id> <permission> &7- revoke a permission node"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/rank perms <id> &7- list every permission on the rank"));
    }

    @Subcommand("list")
    public void list(@NotNull CommandSender sender) {
        List<CoreRank> ranks = rankService.all();
        sender.sendMessage(MythicHex.colorize("&#F529BERanks &7(&f" + ranks.size() + "&7)"));
        for (CoreRank rank : ranks) {
            String color = ampHex(rank.color());
            sender.sendMessage(MythicHex.colorize(
                    "&8• " + color + rank.id() + " &7weight=&f" + rank.weight()
                            + " &7staff=&f" + rank.staff()
                            + " &7parent=&f" + (rank.parent().isEmpty() ? "-" : rank.parent())));
        }
    }

    @Subcommand("info")
    @Complete({"ranks"})
    public void info(@NotNull CommandSender sender, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUnknown rank: &#FFFFFF" + rankId));
            return;
        }
        String color = ampHex(rank.color());
        sender.sendMessage(MythicHex.colorize("&#F529BERank " + color + rank.id()));
        sender.sendMessage(MythicHex.colorize("&7Name: &f" + rank.name()));
        sender.sendMessage(MythicHex.colorize("&7Color: " + color + rank.color()));
        sender.sendMessage(MythicHex.colorize("&7Weight: &f" + rank.weight()));
        sender.sendMessage(MythicHex.colorize("&7Staff: &f" + rank.staff() + " &7Donator: &f" + rank.donator()));
        sender.sendMessage(MythicHex.colorize("&7Parent: &f" + (rank.parent().isEmpty() ? "-" : rank.parent())));
        sender.sendMessage(MythicHex.colorize("&7Prefix: &f" + rank.prefix()));
        sender.sendMessage(MythicHex.colorize("&7Chat-prefix: &f" + rank.chatPrefix()));
        sender.sendMessage(MythicHex.colorize("&7Tab-prefix: &f" + rank.tabPrefix()));
        sender.sendMessage(MythicHex.colorize("&7Nametag-prefix: &f" + rank.nametagPrefix()));
        sender.sendMessage(MythicHex.colorize("&7Permissions: &f" + rank.permissions().size()
                + " &7(use &f/rank perms " + rank.id() + " &7to list)"));
    }

    @Subcommand("create")
    public void create(@NotNull CommandSender sender, @NotNull String rawId, String[] nameWords) {
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty() || !id.matches("[a-z0-9_-]+")) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ARank id must match &#FFFFFF[a-z0-9_-]+&#FF8A8A."));
            return;
        }
        if (rankService.get(id) != null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ARank &#FFFFFF" + id + " &#FF8A8Aalready exists."));
            return;
        }
        String name = (nameWords == null || nameWords.length == 0)
                ? Character.toUpperCase(id.charAt(0)) + id.substring(1)
                : String.join(" ", nameWords);
        CoreRank rank = new CoreRank(
                id, name, "#D2D8E0", Material.LIGHT_GRAY_DYE,
                "&#D2D8E0", "", 1000, false, false, "default",
                List.of(),
                "&#D2D8E0",
                "%chat_prefix%%player%&7: &#FFFFFF%message%",
                "&#D2D8E0", "%tab_prefix%%player%",
                "&#D2D8E0", "%nametag_prefix%%player%");
        rankService.register(rank);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CCreated rank &#FFFFFF" + id + " &7(&f" + name + "&7). "
                        + "Use &f/rank set " + id + " <field> <value> &7to configure."));
    }

    @Subcommand("delete")
    @Complete({"ranks"})
    public void delete(@NotNull CommandSender sender, @NotNull String rankId) {
        deleteInternal(sender, rankId);
    }

    @Subcommand("remove")
    @Complete({"ranks"})
    public void remove(@NotNull CommandSender sender, @NotNull String rankId) {
        deleteInternal(sender, rankId);
    }

    private void deleteInternal(@NotNull CommandSender sender, @NotNull String rankId) {
        String id = rankId.trim().toLowerCase(Locale.ROOT);
        if (id.equals("default")) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AThe default rank cannot be deleted."));
            return;
        }
        if (rankService.get(id) == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUnknown rank: &#FFFFFF" + id));
            return;
        }
        rankService.removeRank(id);
        sender.sendMessage(MythicHex.colorize("&#9CFF9CRemoved rank &#FFFFFF" + id + "&#9CFF9C."));
    }

    @Subcommand("set")
    @Complete({"ranks", "rank-fields"})
    public void set(@NotNull CommandSender sender, @NotNull String rankId, @NotNull String field, @NotNull String[] valueWords) {
        if (valueWords.length == 0) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/rank set <rank> <field> <value...>"));
            return;
        }
        String value = String.join(" ", valueWords);
        if (!rankService.setField(rankId, field, value)) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AFailed to update &#FFFFFF" + rankId + "&#FF8A8A. Check the rank id and field name."));
            return;
        }
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CUpdated &#FFFFFF" + rankId + "&#9CFF9C: &7" + field + " = &f" + value));
    }

    @Subcommand("addperm")
    @Complete({"ranks"})
    public void addPerm(@NotNull CommandSender sender, @NotNull String rankId, @NotNull String permission) {
        if (!rankService.addPermission(rankId, permission)) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8ACould not add. Either the rank is unknown or it already has that permission."));
            return;
        }
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CGranted &#FFFFFF" + permission + " &#9CFF9Cto &#FFFFFF" + rankId + "&#9CFF9C."));
    }

    @Subcommand("removeperm")
    @Complete({"ranks"})
    public void removePerm(@NotNull CommandSender sender, @NotNull String rankId, @NotNull String permission) {
        if (!rankService.removePermission(rankId, permission)) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8ACould not remove. Either the rank is unknown or it doesn't have that permission."));
            return;
        }
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CRevoked &#FFFFFF" + permission + " &#9CFF9Cfrom &#FFFFFF" + rankId + "&#9CFF9C."));
    }

    @Subcommand("perms")
    @Complete({"ranks"})
    public void perms(@NotNull CommandSender sender, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUnknown rank: &#FFFFFF" + rankId));
            return;
        }
        List<String> permissions = rank.permissions();
        sender.sendMessage(MythicHex.colorize(
                "&#F529BEPermissions on &#FFFFFF" + rank.id() + " &7(&f" + permissions.size() + "&7)"));
        if (permissions.isEmpty()) {
            sender.sendMessage(MythicHex.colorize("&8(none)"));
            return;
        }
        for (String perm : permissions) {
            sender.sendMessage(MythicHex.colorize("&8• &f" + perm));
        }
    }

    @NotNull
    private static String ampHex(@NotNull String color) {
        if (color.isBlank()) return "&7";
        return color.startsWith("#") && !color.startsWith("&#") ? "&" + color : color;
    }
}

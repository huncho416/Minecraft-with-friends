package net.mythicpvp.core.chat;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandAlias("filter")
@CommandPermission("mythic.core.chatfilter.manage")
public final class ChatFilterCommand extends MythicCommand {

    private final ChatFilterService service;

    public ChatFilterCommand(@NotNull ChatFilterService service) {
        this.service = service;
    }

    @Default
    public void usage(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#F529BEChat Filter Commands"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter add <title> <literal|regex> <pattern> &7- add a filter (auto-punish on)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter remove <title-or-id> &7- remove a filter"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter list &7- list active filters"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filters &7- open the filter management GUI"));
    }

    @Subcommand("add")
    public void add(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/filter add <title> <literal|regex> <pattern>"));
            return;
        }
        int patternStart;
        ChatFilterEntry.Type type;
        int titleEnd = -1;
        for (int i = args.length - 2; i >= 1; i--) {
            String maybeType = args[i].toUpperCase(Locale.ROOT);
            if (maybeType.equals("LITERAL") || maybeType.equals("REGEX")) {
                titleEnd = i;
                break;
            }
        }
        if (titleEnd <= 0 || titleEnd >= args.length - 1) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AType must be &#FFFFFFliteral &#FF8A8Aor &#FFFFFFregex&#FF8A8A."));
            return;
        }
        String title = String.join(" ", java.util.Arrays.copyOfRange(args, 0, titleEnd));
        type = ChatFilterEntry.Type.valueOf(args[titleEnd].toUpperCase(Locale.ROOT));
        patternStart = titleEnd + 1;
        String pattern = String.join(" ", java.util.Arrays.copyOfRange(args, patternStart, args.length));
        ChatFilterEntry entry = service.add(title, type, pattern, true);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CAdded filter #&f" + entry.id() + " &#9CFF9C(" + entry.title() + ", " + entry.type().name() + ")."));
    }

    @Subcommand("remove")
    public void remove(@NotNull CommandSender sender, @NotNull String titleOrId) {
        long id;
        ChatFilterEntry entry = null;
        try {
            id = Long.parseLong(titleOrId);
            entry = service.get(id);
        } catch (NumberFormatException e) {
            entry = service.findByTitle(titleOrId);
        }
        if (entry == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ANo filter matched &#FFFFFF" + titleOrId + "&#FF8A8A."));
            return;
        }
        service.remove(entry.id());
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CRemoved filter #&f" + entry.id() + " &#9CFF9C(" + entry.title() + ")."));
    }

    @Subcommand("list")
    public void list(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#F529BEActive chat filters:"));
        for (ChatFilterEntry entry : service.all()) {
            sender.sendMessage(MythicHex.colorize(
                    "&8• &f#" + entry.id() + " &#FFFFFF" + entry.title()
                            + " &7[" + entry.type().name() + (entry.autoPunish() ? "" : ", warn-only") + "] "
                            + "&8» &7" + entry.pattern()));
        }
    }

    @CommandAlias("filters")
    @CommandPermission("mythic.core.chatfilter.manage")
    public static final class FiltersCommand extends MythicCommand {
        private final ChatFilterMenu menu;

        public FiltersCommand(@NotNull ChatFilterMenu menu) {
            this.menu = menu;
        }

        @Default
        public void execute(@NotNull Player player) {
            menu.openOverview(player);
        }
    }
}

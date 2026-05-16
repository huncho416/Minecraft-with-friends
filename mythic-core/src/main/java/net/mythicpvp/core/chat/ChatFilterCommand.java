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
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter add <title> <literal|regex> <pattern1>|<pattern2>|... &7- add a filter (| separates multiple patterns)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter addpattern <title-or-id> <pattern> &7- append a pattern to an existing filter"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter removepattern <title-or-id> <pattern> &7- remove one pattern from a filter"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter remove <title-or-id> &7- remove an entire filter"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filter list &7- list active filters"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/filters &7- open the filter management GUI"));
    }

    @Subcommand("add")
    public void add(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/filter add <title> <literal|regex> <pattern1>|<pattern2>|..."));
            return;
        }
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
        ChatFilterEntry.Type type = ChatFilterEntry.Type.valueOf(args[titleEnd].toUpperCase(Locale.ROOT));
        String rawPatterns = String.join(" ", java.util.Arrays.copyOfRange(args, titleEnd + 1, args.length));
        java.util.List<String> patterns = ChatFilterEntry.splitPatterns(rawPatterns);
        if (patterns.isEmpty()) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ANo non-empty patterns supplied."));
            return;
        }
        ChatFilterEntry entry = service.add(title, type, patterns, true);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CAdded filter #&f" + entry.id() + " &#9CFF9C(" + entry.title()
                        + ", " + entry.type().name() + ", " + entry.patterns().size() + " pattern(s))."));
    }

    @Subcommand("addpattern")
    public void addPattern(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/filter addpattern <title-or-id> <pattern>"));
            return;
        }
        ChatFilterEntry entry = lookup(args[0]);
        if (entry == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ANo filter matched &#FFFFFF" + args[0] + "&#FF8A8A."));
            return;
        }
        String pattern = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        entry.addPattern(pattern);
        service.save();
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CAdded pattern to &f" + entry.title() + " &#9CFF9C(now " + entry.patterns().size() + ")."));
    }

    @Subcommand("removepattern")
    public void removePattern(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/filter removepattern <title-or-id> <pattern>"));
            return;
        }
        ChatFilterEntry entry = lookup(args[0]);
        if (entry == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ANo filter matched &#FFFFFF" + args[0] + "&#FF8A8A."));
            return;
        }
        String pattern = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (entry.removePattern(pattern)) {
            service.save();
            sender.sendMessage(MythicHex.colorize(
                    "&#9CFF9CRemoved pattern from &f" + entry.title() + "&#9CFF9C."));
        } else {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AThat pattern is not in this filter."));
        }
    }

    @Subcommand("remove")
    public void remove(@NotNull CommandSender sender, @NotNull String titleOrId) {
        ChatFilterEntry entry = lookup(titleOrId);
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
                            + "&8» &7" + entry.patterns().size() + " pattern(s)"));
        }
    }

    private ChatFilterEntry lookup(@NotNull String input) {
        try {
            return service.get(Long.parseLong(input));
        } catch (NumberFormatException e) {
            return service.findByTitle(input);
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

package net.mythicpvp.core.chat;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ChatFilterMenu {

    private final ChatFilterService service;
    private final ChatPromptService prompts;

    public ChatFilterMenu(@NotNull ChatFilterService service, @NotNull ChatPromptService prompts) {
        this.service = service;
        this.prompts = prompts;
    }

    public void openOverview(@NotNull Player viewer) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BEChat Filters");
        for (ChatFilterEntry entry : service.all()) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Id: &f#" + entry.id());
            lore.add("&7Type: &f" + entry.type().name());
            lore.add("&7Patterns: &f" + entry.patterns().size());
            lore.add("&7Auto-punish: " + (entry.autoPunish() ? "&aon" : "&cwarn-only"));
            lore.add("");
            lore.add("&#9CFF9CLeft-click&7 to edit");
            lore.add("&#FF8A8AShift-Right-click&7 to delete");
            menu.addItem(MythicItem.create(entry.autoPunish() ? Material.RED_WOOL : Material.YELLOW_WOOL)
                    .name("&#FFFFFF" + entry.title())
                    .lore(lore)
                    .build(), event -> {
                if (event.getClick().isShiftClick() && event.getClick().isRightClick()) {
                    service.remove(entry.id());
                    viewer.sendMessage(MythicHex.colorize(
                            "&#9CFF9CRemoved filter &f" + entry.title() + "&#9CFF9C."));
                    openOverview(viewer);
                    return;
                }
                openEdit(viewer, entry);
            });
        }
        menu.staticSlot(49, MythicItem.create(Material.EMERALD)
                        .name("&#9CFF9CAdd new filter")
                        .lore("&7Click and follow chat prompts.")
                        .build(),
                event -> promptAdd(viewer));
        menu.open(viewer);
    }

    private void openEdit(@NotNull Player viewer, @NotNull ChatFilterEntry entry) {
        MythicMenu menu = MythicMenu.create(3, "&#F529BEFilter — &#FFFFFF" + entry.title());
        menu.slot(10, MythicItem.create(Material.NAME_TAG)
                .name("&#FFFFFFTitle: &7" + entry.title())
                .lore("&7Click to change.")
                .build(), event -> {
            viewer.closeInventory();
            viewer.sendMessage(MythicHex.colorize("&#D2D8E0Enter the new title in chat:"));
            prompts.await(viewer, (p, input) -> {
                entry.setTitle(input.trim());
                service.save();
                openEdit(p, entry);
            });
        });
        menu.slot(12, MythicItem.create(Material.COMPARATOR)
                .name("&#FFFFFFType: &7" + entry.type().name())
                .lore("&7Click to toggle LITERAL/REGEX.")
                .build(), event -> {
            entry.setType(entry.type() == ChatFilterEntry.Type.LITERAL
                    ? ChatFilterEntry.Type.REGEX : ChatFilterEntry.Type.LITERAL);
            service.save();
            openEdit(viewer, entry);
        });
        menu.slot(14, MythicItem.create(Material.WRITABLE_BOOK)
                .name("&#FFFFFFPatterns: &7" + entry.patterns().size())
                .lore(
                        "&#9CFF9CLeft-click&7 to add a pattern.",
                        "&#FF8A8ARight-click&7 to view/remove patterns.")
                .build(), event -> {
            if (event.getClick().isRightClick()) {
                openPatternList(viewer, entry);
            } else {
                promptAddPattern(viewer, entry);
            }
        });
        menu.slot(16, MythicItem.create(entry.autoPunish() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&#FFFFFFAuto-punish: " + (entry.autoPunish() ? "&aon" : "&cwarn-only"))
                .lore("&7Click to toggle. When off, players are warned but never auto-muted.")
                .build(), event -> {
            entry.setAutoPunish(!entry.autoPunish());
            service.save();
            openEdit(viewer, entry);
        });
        menu.slot(22, MythicItem.create(Material.BARRIER).name("&#FF8A8ABack").build(),
                event -> openOverview(viewer));
        menu.open(viewer);
    }

    private void openPatternList(@NotNull Player viewer, @NotNull ChatFilterEntry entry) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BEPatterns — &#FFFFFF" + entry.title());
        List<String> patterns = entry.patterns();
        for (int i = 0; i < patterns.size(); i++) {
            String pattern = patterns.get(i);
            final int index = i;
            menu.addItem(MythicItem.create(Material.PAPER)
                    .name("&#FFFFFF" + truncate(pattern, 36))
                    .lore(
                            "&7Index: &f" + (i + 1),
                            "",
                            "&#FF8A8AShift-Right-click&7 to remove")
                    .build(), event -> {
                if (event.getClick().isShiftClick() && event.getClick().isRightClick()) {
                    entry.removePatternAt(index);
                    service.save();
                    openPatternList(viewer, entry);
                }
            });
        }
        menu.staticSlot(45, MythicItem.create(Material.EMERALD)
                        .name("&#9CFF9CAdd pattern")
                        .build(),
                event -> promptAddPattern(viewer, entry));
        menu.staticSlot(49, MythicItem.create(Material.BARRIER).name("&#FF8A8ABack").build(),
                event -> openEdit(viewer, entry));
        menu.open(viewer);
    }

    private void promptAddPattern(@NotNull Player viewer, @NotNull ChatFilterEntry entry) {
        viewer.closeInventory();
        viewer.sendMessage(MythicHex.colorize(
                "&#D2D8E0Enter one or more patterns in chat (separate with &#FFFFFF| &#D2D8E0for multiple):"));
        prompts.await(viewer, (p, input) -> {
            List<String> next = ChatFilterEntry.splitPatterns(input);
            if (next.isEmpty()) {
                p.sendMessage(MythicHex.colorize("&#FF8A8ANo non-empty patterns supplied."));
                openPatternList(p, entry);
                return;
            }
            for (String pattern : next) {
                entry.addPattern(pattern);
            }
            service.save();
            p.sendMessage(MythicHex.colorize(
                    "&#9CFF9CAdded &f" + next.size() + " &#9CFF9Cpattern(s) to &f" + entry.title() + "&#9CFF9C."));
            openPatternList(p, entry);
        });
    }

    private void promptAdd(@NotNull Player viewer) {
        viewer.closeInventory();
        viewer.sendMessage(MythicHex.colorize("&#D2D8E0Enter the filter title:"));
        prompts.await(viewer, (p1, title) -> {
            p1.sendMessage(MythicHex.colorize("&#D2D8E0Enter type &7(literal/regex):"));
            prompts.await(p1, (p2, typeIn) -> {
                ChatFilterEntry.Type type;
                try {
                    type = ChatFilterEntry.Type.valueOf(typeIn.trim().toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    p2.sendMessage(MythicHex.colorize("&#FF8A8AInvalid type. Cancelled."));
                    return;
                }
                p2.sendMessage(MythicHex.colorize(
                        "&#D2D8E0Enter one or more patterns (separate with &#FFFFFF| &#D2D8E0for multiple):"));
                prompts.await(p2, (p3, pattern) -> {
                    List<String> patterns = ChatFilterEntry.splitPatterns(pattern);
                    if (patterns.isEmpty()) {
                        p3.sendMessage(MythicHex.colorize("&#FF8A8ANo non-empty patterns supplied. Cancelled."));
                        return;
                    }
                    ChatFilterEntry entry = service.add(title.trim(), type, patterns, true);
                    p3.sendMessage(MythicHex.colorize(
                            "&#9CFF9CAdded filter &f" + entry.title() + " &#9CFF9C(#"
                                    + entry.id() + ", " + entry.patterns().size() + " pattern(s))."));
                    openOverview(p3);
                });
            });
        });
    }

    @NotNull
    private static String truncate(@NotNull String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max - 1) + "…";
    }

    @NotNull
    public List<ChatFilterEntry> filters() {
        return service.all();
    }
}

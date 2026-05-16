package net.mythicpvp.core.chat;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            menu.addItem(MythicItem.create(entry.autoPunish() ? Material.RED_WOOL : Material.YELLOW_WOOL)
                    .name("&#FFFFFF" + entry.title())
                    .lore(
                            "&7Id: &f#" + entry.id(),
                            "&7Type: &f" + entry.type().name(),
                            "&7Pattern: &f" + truncate(entry.pattern(), 40),
                            "&7Auto-punish: " + (entry.autoPunish() ? "&aon" : "&cwarn-only"),
                            "",
                            "&#9CFF9CLeft-click&7 to edit",
                            "&#FF8A8AShift-Right-click&7 to delete")
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
        MythicMenu menu = MythicMenu.create(3, "&#F529BEFilter &7— &#FFFFFF" + entry.title());
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
                .name("&#FFFFFFPattern: &7" + truncate(entry.pattern(), 24))
                .lore("&7Click and enter a new pattern in chat.")
                .build(), event -> {
            viewer.closeInventory();
            viewer.sendMessage(MythicHex.colorize("&#D2D8E0Enter the new pattern in chat:"));
            prompts.await(viewer, (p, input) -> {
                entry.setPattern(input.trim());
                service.save();
                openEdit(p, entry);
            });
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
                p2.sendMessage(MythicHex.colorize("&#D2D8E0Enter the pattern:"));
                prompts.await(p2, (p3, pattern) -> {
                    ChatFilterEntry entry = service.add(title.trim(), type, pattern.trim(), true);
                    p3.sendMessage(MythicHex.colorize(
                            "&#9CFF9CAdded filter &f" + entry.title() + " &#9CFF9C(#"
                                    + entry.id() + ")."));
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

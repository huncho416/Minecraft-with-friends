package net.mythicpvp.core.rank;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class RankEditorMenuService {

    private final RankService rankService;
    private final ChatPromptService prompts;
    private final RankMenuText text;

    public RankEditorMenuService(@NotNull RankService rankService,
                                 @NotNull ChatPromptService prompts,
                                 @NotNull RankMenuText text) {
        this.rankService = rankService;
        this.prompts = prompts;
        this.text = text;
    }

    public void openRankList(@NotNull Player viewer) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BERank Editor");
        for (CoreRank rank : rankService.all()) {
            menu.addItem(MythicItem.create(rank.dye())
                    .name(asAmpHex(rank.color()) + rank.name())
                    .lore(List.of(
                            "&7Id: &f" + rank.id(),
                            "&7Weight: &f" + rank.weight(),
                            "&7Staff: &f" + (rank.staff() ? "Yes" : "No"),
                            "&#F529BEClick to edit"))
                    .build(), event -> openOverview(viewer, rank.id()));
        }
        menu.open(viewer);
    }

    public void openOverview(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu.create(3, text.editorTitle(rank.id()))
                .slot(10, MythicItem.create(rank.dye())
                        .name(asAmpHex(rank.color()) + rank.name())
                        .lore(List.of(
                                "&7Weight: &f" + rank.weight(),
                                "&7Staff: &f" + rank.staff(),
                                "&7Donator: &f" + rank.donator(),
                                "&7Parent: &f" + (rank.parent().isBlank() ? "None" : rank.parent()),
                                "&#F529BEClick to edit fields"))
                        .build(), event -> openFieldEditor(viewer, rankId))
                .slot(12, MythicItem.create(Material.NAME_TAG)
                        .name(text.editorDisplayFormats())
                        .lore(List.of(
                                "&7Chat: &f" + sanitizeColors(rank.chatPrefix()),
                                "&7Tab: &f" + sanitizeColors(rank.tabPrefix()),
                                "&7Nametag: &f" + sanitizeColors(rank.nametagPrefix()),
                                "&#F529BEClick to edit"))
                        .build(), event -> openFormatEditor(viewer, rankId))
                .slot(14, MythicItem.create(Material.BOOK)
                        .name(text.editorPermissions())
                        .lore(loreWithCount(rank.permissions()))
                        .build(), event -> openPermissionMenu(viewer, rankId))
                .slot(16, MythicItem.create(Material.BARRIER)
                        .name(text.editorClose()).build(),
                        event -> viewer.closeInventory())
                .open(viewer);
    }

    public void openFieldEditor(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu menu = MythicMenu.create(4, text.editorFieldsTitle());

        addPromptField(menu, 10, Material.NAME_TAG, "Name", "name", rank.name(), viewer, rankId);
        addPromptField(menu, 11, Material.RED_DYE, "Color", "color", rank.color(), viewer, rankId);
        addPromptField(menu, 12, Material.LIGHT_GRAY_DYE, "Dye material", "dye", rank.dye().name(), viewer, rankId);
        addPromptField(menu, 13, Material.CLOCK, "Weight", "weight", Integer.toString(rank.weight()), viewer, rankId);
        addPromptField(menu, 14, Material.PAPER, "Prefix", "prefix", rank.prefix(), viewer, rankId);
        addPromptField(menu, 15, Material.PAPER, "Suffix", "suffix", rank.suffix().isBlank() ? "(empty)" : rank.suffix(), viewer, rankId);
        addPromptField(menu, 16, Material.LADDER, "Parent rank", "parent", rank.parent().isBlank() ? "(none)" : rank.parent(), viewer, rankId);
        addToggleField(menu, 19, Material.LIME_DYE, Material.GRAY_DYE, "Staff", "staff", rank.staff(), viewer, rankId);
        addToggleField(menu, 20, Material.LIME_DYE, Material.GRAY_DYE, "Donator", "donator", rank.donator(), viewer, rankId);
        menu.slot(31, MythicItem.create(Material.ARROW).name(text.editorBack()).build(),
                event -> openOverview(viewer, rankId));
        menu.open(viewer);
    }

    public void openFormatEditor(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu menu = MythicMenu.create(4, text.editorFormatsTitle());
        java.util.Map<String, String> placeholders = formatPreviewPlaceholders(viewer, rank);
        addPromptField(menu, 10, Material.WRITABLE_BOOK, "Chat prefix", "chat-prefix",
                resolveAndSanitize(rank.chatPrefix(), placeholders), viewer, rankId);
        addPromptField(menu, 11, Material.BOOK, "Chat format", "chat-format",
                resolveAndSanitize(rank.chatFormat(), placeholders), viewer, rankId);
        addPromptField(menu, 13, Material.WRITABLE_BOOK, "Tab prefix", "tab-prefix",
                resolveAndSanitize(rank.tabPrefix(), placeholders), viewer, rankId);
        addPromptField(menu, 14, Material.BOOK, "Tab format", "tab-format",
                resolveAndSanitize(rank.tabFormat(), placeholders), viewer, rankId);
        addPromptField(menu, 16, Material.WRITABLE_BOOK, "Nametag prefix", "nametag-prefix",
                resolveAndSanitize(rank.nametagPrefix(), placeholders), viewer, rankId);
        addPromptField(menu, 17, Material.BOOK, "Nametag format", "nametag-format",
                resolveAndSanitize(rank.nametagFormat(), placeholders), viewer, rankId);
        menu.slot(31, MythicItem.create(Material.ARROW).name(text.editorBack()).build(),
                event -> openOverview(viewer, rankId));
        menu.open(viewer);
    }

    public void openPermissionMenu(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        PaginatedMenu menu = PaginatedMenu.create(6, text.editorPermissionsTitle(rank.id()));
        for (String permission : rank.permissions()) {
            menu.addItem(MythicItem.create(Material.PAPER)
                    .name("&#FFFFFF" + permission)
                    .lore(text.editorRemovePermissionLore())
                    .build(), event -> {
                        rankService.removePermission(rankId, permission);

                        openPermissionMenu(viewer, rankId);
                    });
        }
        menu.addItem(MythicItem.create(Material.LIME_WOOL)
                .name(text.editorAddPermission())
                .lore(text.editorAddPermissionLore())
                .build(), event -> prompts.await(viewer, (player, input) -> {
                    String permission = input.trim();
                    if (!permission.isEmpty()) {
                        rankService.addPermission(rankId, permission);
                    }
                    openPermissionMenu(player, rankId);
                }));
        int backSlot = 6 * 9 - 5;
        menu.staticSlot(backSlot,
                MythicItem.create(Material.ARROW).name(text.editorBack()).build(),
                event -> openOverview(viewer, rankId));
        menu.open(viewer);
    }

    private void addPromptField(@NotNull MythicMenu menu, int slot, @NotNull Material icon,
                                @NotNull String label, @NotNull String fieldKey,
                                @NotNull String currentValue, @NotNull Player viewer,
                                @NotNull String rankId) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&7Current: &f" + sanitizeColors(currentValue));
        lore.add(text.editorFieldPrompt());
        if ("parent".equals(fieldKey)) {
            lore.add("&#FF8A8ARight-click to remove parent.");
        }
        menu.slot(slot, MythicItem.create(icon)
                .name("&#F529BE" + label)
                .lore(lore)
                .build(), event -> {
                    if ("parent".equals(fieldKey) && event.getClick() != null && event.getClick().isRightClick()) {
                        boolean ok = rankService.setField(rankId, "parent", "");
                        viewer.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(ok
                                ? "&#9CFF9CParent rank removed."
                                : "&#FF8A8AFailed to remove parent."));
                        openFieldEditor(viewer, rankId);
                        return;
                    }
                    prompts.await(viewer, (player, input) -> {
                        boolean ok = rankService.setField(rankId, fieldKey, input);
                        player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(ok
                                ? text.editorFieldUpdated(label, input)
                                : text.editorFieldFailed(label)));
                        openFieldEditor(player, rankId);
                    });
                });
    }

    @NotNull
    private static String resolveAndSanitize(@NotNull String template,
                                             @NotNull java.util.Map<String, String> placeholders) {
        String result = template;
        for (java.util.Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return sanitizeColors(result);
    }

    @NotNull
    private static java.util.Map<String, String> formatPreviewPlaceholders(@NotNull Player viewer,
                                                                          @NotNull CoreRank rank) {
        return java.util.Map.of(
                "player", viewer.getName(),
                "message", "<message>",
                "chat_prefix", rank.chatPrefix(),
                "tab_prefix", rank.tabPrefix(),
                "nametag_prefix", rank.nametagPrefix(),
                "prefix", rank.prefix(),
                "suffix", rank.suffix(),
                "rank", rank.name(),
                "rank_id", rank.id(),
                "rank_color", rank.color());
    }

    @NotNull
    static String asAmpHex(@NotNull String input) {
        if (input.startsWith("#") && !input.startsWith("&#")) {
            return "&" + input;
        }
        return input;
    }

    @NotNull
    static String sanitizeColors(@NotNull String input) {
        return input.replaceAll("(?<!&)#([A-Fa-f0-9]{6})", "&#$1");
    }

    private void addToggleField(@NotNull MythicMenu menu, int slot,
                                @NotNull Material onIcon, @NotNull Material offIcon,
                                @NotNull String label, @NotNull String fieldKey,
                                boolean current, @NotNull Player viewer,
                                @NotNull String rankId) {
        menu.slot(slot, MythicItem.create(current ? onIcon : offIcon)
                .name("&#F529BE" + label)
                .lore(List.of(
                        "&7State: &f" + (current ? "Yes" : "No"),
                        "&#F529BEClick to toggle"))
                .build(), event -> {
                    rankService.setField(rankId, fieldKey, Boolean.toString(!current));
                    openFieldEditor(viewer, rankId);
                });
    }

    @NotNull
    private static List<String> loreWithCount(@NotNull List<String> permissions) {
        List<String> out = new ArrayList<>();
        out.add("&7Count: &f" + permissions.size());
        out.add("&#F529BEClick to manage");
        return out;
    }
}

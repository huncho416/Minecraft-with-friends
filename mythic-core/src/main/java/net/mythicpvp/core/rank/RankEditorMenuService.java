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

/**
 * Click-driven editor for {@link CoreRank} fields.
 *
 * <p>The original `/rankeditor <rank>` opens a small read-only display.
 * This service adds the click flows: tapping a field button opens a chat
 * prompt (or a child menu) that mutates the rank via
 * {@link RankService#setField}, {@link RankService#addPermission}, or
 * {@link RankService#removePermission}. Each mutation runs through the
 * existing rank-update path so STDB persistence + display refresh fire
 * automatically.
 *
 * <p>Threading: every interaction starts on the Bukkit primary thread
 * (inventory click event). Chat prompts hop to the prompt service which
 * already routes back to main before invoking the callback.
 */
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

    /** Top-level overview — same shape as RankEditorCommand's read-only menu. */
    public void openOverview(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu.create(3, text.editorTitle(rank.id()))
                .slot(10, MythicItem.create(rank.dye())
                        .name(rank.color() + rank.name())
                        .lore(List.of(
                                "&7Weight: &f" + rank.weight(),
                                "&7Staff: &f" + rank.staff(),
                                "&7Donator: &f" + rank.donator(),
                                "&7Parent: &f" + (rank.parent().isBlank() ? "None" : rank.parent()),
                                "&#FF00F8Click to edit fields"))
                        .build(), event -> openFieldEditor(viewer, rankId))
                .slot(12, MythicItem.create(Material.NAME_TAG)
                        .name(text.editorDisplayFormats())
                        .lore(List.of(
                                "&7Chat: &f" + rank.chatPrefix(),
                                "&7Tab: &f" + rank.tabPrefix(),
                                "&7Nametag: &f" + rank.nametagPrefix(),
                                "&#FF00F8Click to edit"))
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

    /**
     * Page of one-click field editors. Each button opens a chat prompt
     * via {@link ChatPromptService}; the response is routed back to
     * {@link RankService#setField}.
     */
    public void openFieldEditor(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu menu = MythicMenu.create(4, text.editorFieldsTitle());
        // Field grid layout: row 1 — identity (name, color, dye, weight),
        // row 2 — boolean toggles (staff, donator) + parent + suffix.
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

    /** Editor for chat/tab/nametag prefix + format strings. */
    public void openFormatEditor(@NotNull Player viewer, @NotNull String rankId) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            viewer.sendMessage("Unknown rank.");
            return;
        }
        MythicMenu menu = MythicMenu.create(4, text.editorFormatsTitle());
        addPromptField(menu, 10, Material.WRITABLE_BOOK, "Chat prefix", "chat-prefix", rank.chatPrefix(), viewer, rankId);
        addPromptField(menu, 11, Material.BOOK, "Chat format", "chat-format", rank.chatFormat(), viewer, rankId);
        addPromptField(menu, 13, Material.WRITABLE_BOOK, "Tab prefix", "tab-prefix", rank.tabPrefix(), viewer, rankId);
        addPromptField(menu, 14, Material.BOOK, "Tab format", "tab-format", rank.tabFormat(), viewer, rankId);
        addPromptField(menu, 16, Material.WRITABLE_BOOK, "Nametag prefix", "nametag-prefix", rank.nametagPrefix(), viewer, rankId);
        addPromptField(menu, 17, Material.BOOK, "Nametag format", "nametag-format", rank.nametagFormat(), viewer, rankId);
        menu.slot(31, MythicItem.create(Material.ARROW).name(text.editorBack()).build(),
                event -> openOverview(viewer, rankId));
        menu.open(viewer);
    }

    /**
     * Permission menu — pages through the rank's permissions; each entry
     * is a click-to-remove button. A separate "add" button opens a chat
     * prompt.
     */
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
                        // Re-open the menu so the now-removed permission disappears.
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
        menu.open(viewer);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Build a "click-to-prompt" tile. Clicking opens a chat prompt; the
     * response is dispatched to {@link RankService#setField}. The user
     * gets a feedback message and is returned to the field editor.
     */
    private void addPromptField(@NotNull MythicMenu menu, int slot, @NotNull Material icon,
                                @NotNull String label, @NotNull String fieldKey,
                                @NotNull String currentValue, @NotNull Player viewer,
                                @NotNull String rankId) {
        menu.slot(slot, MythicItem.create(icon)
                .name("&#FF00F8" + label)
                .lore(List.of(
                        "&7Current: &f" + currentValue,
                        text.editorFieldPrompt()))
                .build(), event -> prompts.await(viewer, (player, input) -> {
                    boolean ok = rankService.setField(rankId, fieldKey, input);
                    player.sendMessage(ok
                            ? text.editorFieldUpdated(label, input)
                            : text.editorFieldFailed(label));
                    openFieldEditor(player, rankId);
                }));
    }

    /**
     * Build a click-to-toggle boolean field. No chat prompt — the click
     * itself flips the value via {@link RankService#setField}.
     */
    private void addToggleField(@NotNull MythicMenu menu, int slot,
                                @NotNull Material onIcon, @NotNull Material offIcon,
                                @NotNull String label, @NotNull String fieldKey,
                                boolean current, @NotNull Player viewer,
                                @NotNull String rankId) {
        menu.slot(slot, MythicItem.create(current ? onIcon : offIcon)
                .name("&#FF00F8" + label)
                .lore(List.of(
                        "&7State: &f" + (current ? "Yes" : "No"),
                        "&#FF00F8Click to toggle"))
                .build(), event -> {
                    rankService.setField(rankId, fieldKey, Boolean.toString(!current));
                    openFieldEditor(viewer, rankId);
                });
    }

    @NotNull
    private static List<String> loreWithCount(@NotNull List<String> permissions) {
        List<String> out = new ArrayList<>();
        out.add("&7Count: &f" + permissions.size());
        out.add("&#FF00F8Click to manage");
        return out;
    }
}

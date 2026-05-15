package net.mythicpvp.core.punishment;

import net.mythicpvp.core.note.NoteService;
import net.mythicpvp.core.note.PlayerNote;
import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ManagePunishmentsMenuService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final PunishmentService punishmentService;
    private final NoteService noteService;
    private final ChatPromptService prompts;

    public ManagePunishmentsMenuService(@NotNull PunishmentService punishmentService,
                                        @NotNull NoteService noteService,
                                        @NotNull ChatPromptService prompts) {
        this.punishmentService = punishmentService;
        this.noteService = noteService;
        this.prompts = prompts;
    }

    public void openOverview(@NotNull Player viewer) {
        MythicMenu menu = MythicMenu.create(3, "&#F529BEManage Punishments");
        int slot = 10;
        for (PunishmentType type : PunishmentType.values()) {
            int count = punishmentService.byType(type).size();
            menu.slot(slot, MythicItem.create(materialFor(type))
                    .name(colorFor(type) + displayName(type))
                    .lore(
                            "&7Total records: &f" + count,
                            "",
                            "&#9CFF9CClick to browse.")
                    .build(), event -> openCategory(viewer, type, 0));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        menu.slot(22, MythicItem.create(Material.NAME_TAG)
                .name("&#FFEC8AView a player's full history")
                .lore(
                        "&7Click and enter a player name in chat",
                        "&7to see every punishment, warn, mute, kick,",
                        "&7and note tied to that player.",
                        "",
                        "&#9CFF9CClick to start.")
                .build(), event -> promptPlayerHistory(viewer));
        menu.open(viewer);
    }

    private void openCategory(@NotNull Player viewer, @NotNull PunishmentType type, int page) {
        PaginatedMenu menu = PaginatedMenu.create(6, colorFor(type) + displayName(type));
        long now = System.currentTimeMillis();
        for (PunishmentRecord record : punishmentService.byType(type)) {
            menu.addItem(buildRecordItem(record, now));
        }
        menu.staticSlot(49, MythicItem.create(Material.BARRIER).name("&#FF8A8ABack").build(),
                event -> openOverview(viewer));
        menu.open(viewer, page);
    }

    private void promptPlayerHistory(@NotNull Player viewer) {
        viewer.closeInventory();
        viewer.sendMessage(MythicHex.colorize(
                "&#D2D8E0Enter a player name in chat (or type &#FFFFFFcancel&#D2D8E0):"));
        prompts.await(viewer, (p, input) -> {
            String name = input.trim();
            if (name.isEmpty()) {
                p.sendMessage(MythicHex.colorize("&#FF8A8APlayer name cannot be empty."));
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(name);
            UUID targetUuid = target.getUniqueId();
            String resolvedName = target.getName() == null ? name : target.getName();
            openPlayerHistory(p, targetUuid, resolvedName);
        });
    }

    public void openPlayerHistory(@NotNull Player viewer, @NotNull UUID targetUuid, @NotNull String targetName) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BEHistory &7— &#FFFFFF" + targetName);
        long now = System.currentTimeMillis();
        List<ItemStack> entries = new ArrayList<>();
        for (PunishmentRecord record : punishmentService.history(targetUuid)) {
            entries.add(buildRecordItem(record, now));
        }
        for (PlayerNote note : noteService.notesFor(targetUuid)) {
            entries.add(buildNoteItem(note));
        }
        if (entries.isEmpty()) {
            viewer.sendMessage(MythicHex.colorize(
                    "&#FFEC8ANo punishments or notes recorded for &#FFFFFF" + targetName + "&#FFEC8A."));
            return;
        }
        menu.addItems(entries);
        menu.staticSlot(49, MythicItem.create(Material.BARRIER).name("&#FF8A8ABack").build(),
                event -> openOverview(viewer));
        menu.open(viewer, 0);
    }

    @NotNull
    private static ItemStack buildRecordItem(@NotNull PunishmentRecord record, long now) {
        boolean active = record.active(now);
        Material mat = active ? materialFor(record.type()) : Material.GRAY_DYE;
        return MythicItem.create(mat)
                .name(colorFor(record.type()) + displayName(record.type()) + " &7— &#FFFFFF" + record.targetName())
                .lore(
                        "&7Reason: &f" + (record.reason().isEmpty() ? "(none)" : record.reason()),
                        "&7Issued by: &f" + record.staffName(),
                        "&7Server: &f" + record.server(),
                        "&7Issued at: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(record.createdAtMillis())),
                        record.expiresAtMillis() > 0
                                ? "&7Expires: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(record.expiresAtMillis()))
                                : "&7Expires: &fpermanent",
                        record.pardoned() ? "&7Status: &cPardoned" : (active ? "&7Status: &aActive" : "&7Status: &8Expired"),
                        "&7Record id: &f#" + record.id())
                .build();
    }

    @NotNull
    private static ItemStack buildNoteItem(@NotNull PlayerNote note) {
        return MythicItem.create(Material.PAPER)
                .name("&#9CFF9CNote &7— &#FFFFFF" + note.title())
                .lore(
                        "&7" + note.body(),
                        "",
                        "&7Author: &f" + note.authorName(),
                        "&7Server: &f" + note.serverId(),
                        "&7Created: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(note.createdAt())),
                        "&7Status: " + (note.active() ? "&aActive" : "&8Inactive"),
                        "&7Note id: &f#" + note.id())
                .build();
    }

    @NotNull
    private static Material materialFor(@NotNull PunishmentType type) {
        return switch (type) {
            case BAN -> Material.RED_WOOL;
            case TEMP_BAN -> Material.PINK_WOOL;
            case MUTE -> Material.ORANGE_WOOL;
            case TEMP_MUTE -> Material.YELLOW_WOOL;
            case BLACKLIST -> Material.BLACK_WOOL;
            case WARN -> Material.WHITE_WOOL;
        };
    }

    @NotNull
    private static String colorFor(@NotNull PunishmentType type) {
        return switch (type) {
            case BAN -> "&#FF4444";
            case TEMP_BAN -> "&#FF8A8A";
            case MUTE -> "&#FFA64D";
            case TEMP_MUTE -> "&#FFEC8A";
            case BLACKLIST -> "&#666666";
            case WARN -> "&#FFFFFF";
        };
    }

    @NotNull
    private static String displayName(@NotNull PunishmentType type) {
        return switch (type) {
            case BAN -> "Ban";
            case TEMP_BAN -> "Temp Ban";
            case MUTE -> "Mute";
            case TEMP_MUTE -> "Temp Mute";
            case BLACKLIST -> "Blacklist";
            case WARN -> "Warn";
        };
    }
}

package net.mythicpvp.core.note;

import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class NoteMenuService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final NoteService noteService;

    public NoteMenuService(@NotNull NoteService noteService) {
        this.noteService = noteService;
    }

    public void openFor(@NotNull Player staff, @NotNull UUID targetUuid, @NotNull String targetName) {
        openFor(staff, targetUuid, targetName, 0);
    }

    public void openFor(@NotNull Player staff, @NotNull UUID targetUuid, @NotNull String targetName, int page) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BENotes &7— &#FFFFFF" + targetName);
        List<PlayerNote> all = noteService.notesFor(targetUuid);
        for (PlayerNote note : all) {
            menu.addItem(buildNoteItem(note), event -> {
                if (note.active() && event.getClick().isLeftClick()) {
                    note.setActive(false);
                    staff.sendMessage(MythicHex.colorize(
                            "&#FFEC8ANote &#FFFFFF" + note.title() + " &#FFEC8Amarked inactive."));
                    openFor(staff, targetUuid, targetName, 0);
                } else if (!note.active() && event.getClick().isRightClick()) {
                    noteService.delete(note.id());
                    staff.sendMessage(MythicHex.colorize(
                            "&#FF8A8ARemoved note &#FFFFFF" + note.title() + "&#FF8A8A."));
                    openFor(staff, targetUuid, targetName, 0);
                }
            });
        }
        menu.open(staff, page);
    }

    @NotNull
    private static ItemStack buildNoteItem(@NotNull PlayerNote note) {
        Material icon = note.active() ? Material.PAPER : Material.MAP;
        String header = note.active() ? "&#9CFF9C" : "&#888888";
        return MythicItem.create(icon)
                .name(header + note.title())
                .lore(
                        "&7" + note.body(),
                        "",
                        "&7Author: &f" + note.authorName(),
                        "&7Server: &f" + note.serverId(),
                        "&7Created: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(note.createdAt())),
                        "&7Status: " + (note.active() ? "&aActive" : "&8Inactive"),
                        "&7Note id: &f#" + note.id(),
                        "",
                        note.active()
                                ? "&#FFEC8ALeft-click to set inactive."
                                : "&#FF8A8ARight-click to remove from menu.")
                .build();
    }
}

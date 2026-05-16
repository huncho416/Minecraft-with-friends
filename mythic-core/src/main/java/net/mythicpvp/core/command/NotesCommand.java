package net.mythicpvp.core.command;

import net.mythicpvp.core.note.NoteMenuService;
import net.mythicpvp.core.note.NoteService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@CommandAlias("notes")
@CommandPermission("mythic.core.notes")
public final class NotesCommand extends MythicCommand {

    private final NoteService noteService;
    private final NoteMenuService menuService;

    public NotesCommand(@NotNull NoteService noteService, @NotNull NoteMenuService menuService) {
        this.noteService = noteService;
        this.menuService = menuService;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player staff, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            staff.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/notes <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? targetName : target.getName();
        menuService.openFor(staff, uuid, name);
    }

    @Subcommand("clear")
    @CommandPermission("mythic.core.notes.clear")
    @Complete({"players"})
    public void clear(@NotNull Player staff, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            staff.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/notes clear <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        int removed = noteService.clearFor(target.getUniqueId());
        staff.sendMessage(MythicHex.colorize(
                "&#FF8A8ACleared &#FFFFFF" + removed + " &#FF8A8Anote(s) for &#FFFFFF"
                        + (target.getName() == null ? targetName : target.getName())
                        + "&#FF8A8A."));
    }
}

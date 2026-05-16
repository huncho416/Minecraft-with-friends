package net.mythicpvp.core.command;

import net.mythicpvp.core.note.NoteService;
import net.mythicpvp.core.note.PlayerNote;
import net.mythicpvp.core.prompt.ChatPromptService;
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

@CommandAlias("note")
@CommandPermission("mythic.core.notes")
public final class NoteCommand extends MythicCommand {

    private final NoteService noteService;
    private final ChatPromptService prompts;
    private final String localShardId;

    public NoteCommand(@NotNull NoteService noteService,
                       @NotNull ChatPromptService prompts,
                       @NotNull String localShardId) {
        this.noteService = noteService;
        this.prompts = prompts;
        this.localShardId = localShardId;
    }

    @Default
    public void usage(@NotNull Player staff) {
        staff.sendMessage(MythicHex.colorize("&#F529BENote Commands"));
        staff.sendMessage(MythicHex.colorize("&#FFFFFF/note add <player> &7- add a new note (chat-prompts for title and body)"));
        staff.sendMessage(MythicHex.colorize("&#FFFFFF/note remove <player> <title> &7- remove a note by title"));
        staff.sendMessage(MythicHex.colorize("&#FFFFFF/notes <player> &7- open the notes menu for a player"));
        staff.sendMessage(MythicHex.colorize("&#FFFFFF/notes clear <player> &7- wipe every note for a player (admin only)"));
    }

    @Subcommand("add")
    @Complete({"players"})
    public void add(@NotNull Player staff, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            staff.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/note add <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        java.util.UUID targetUuid = target.getUniqueId();
        String resolvedName = target.getName() == null ? targetName : target.getName();
        staff.sendMessage(MythicHex.colorize(
                "&#D2D8E0Enter the note &#FFFFFFtitle&#D2D8E0 in chat (or type &#FFFFFFcancel&#D2D8E0):"));
        prompts.await(staff, (p, titleInput) -> {
            String title = titleInput.trim();
            if (title.isEmpty()) {
                p.sendMessage(MythicHex.colorize("&#FF8A8ATitle cannot be empty."));
                return;
            }
            p.sendMessage(MythicHex.colorize(
                    "&#D2D8E0Enter the note &#FFFFFFbody&#D2D8E0 in chat (or type &#FFFFFFcancel&#D2D8E0):"));
            prompts.await(p, (p2, bodyInput) -> {
                String body = bodyInput.trim();
                if (body.isEmpty()) {
                    p2.sendMessage(MythicHex.colorize("&#FF8A8ABody cannot be empty."));
                    return;
                }
                PlayerNote note = noteService.add(targetUuid, resolvedName, p2.getUniqueId(),
                        p2.getName(), title, body, localShardId);
                p2.sendMessage(MythicHex.colorize(
                        "&#9CFF9CAdded note &#FFFFFF" + note.title()
                                + " &#9CFF9Cfor &#FFFFFF" + resolvedName + "&#9CFF9C."));
            });
        });
    }

    @Subcommand("remove")
    @Complete({"players"})
    public void remove(@NotNull Player staff, String targetName, String[] titleWords) {
        if (targetName == null || targetName.isBlank() || titleWords == null || titleWords.length == 0) {
            staff.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/note remove <player> <title>"));
            return;
        }
        String title = String.join(" ", titleWords).trim();
        if (title.isEmpty()) {
            staff.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/note remove <player> <title>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        PlayerNote note = noteService.findByTitle(target.getUniqueId(), title);
        if (note == null) {
            staff.sendMessage(MythicHex.colorize(
                    "&#FF8A8ANo note titled &#FFFFFF" + title + " &#FF8A8Afound for that player."));
            return;
        }
        noteService.delete(note.id());
        staff.sendMessage(MythicHex.colorize(
                "&#9CFF9CRemoved note &#FFFFFF" + note.title() + "&#9CFF9C."));
    }
}

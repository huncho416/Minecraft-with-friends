package net.mythicpvp.core.command;

import net.mythicpvp.core.announce.BroadcastService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /broadcast <message…>} — fan a single line of staff text to
 * every player on every server in the network.
 *
 * <p>Permission: {@code mythic.core.broadcast}. Console-friendly so it
 * can run from rcon and from scripted hooks.
 *
 * <p>The {@code String[]} vararg captures everything after the command
 * name; the message preserves spaces and any embedded
 * {@code &#RRGGBB} hex sequences (rendered via Mythic's hex parser
 * downstream in {@link BroadcastService#broadcast}).
 */
@CommandAlias("broadcast")
@CommandPermission("mythic.core.broadcast")
public final class BroadcastCommand extends MythicCommand {

    private final BroadcastService broadcast;

    public BroadcastCommand(@NotNull BroadcastService broadcast) {
        this.broadcast = broadcast;
    }

    @Default
    public void execute(@NotNull CommandSender sender, @NotNull String[] words) {
        if (words.length == 0) {
            sender.sendMessage("Usage: /broadcast <message...>");
            return;
        }
        broadcast.broadcast(String.join(" ", words));
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.announce.BroadcastService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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

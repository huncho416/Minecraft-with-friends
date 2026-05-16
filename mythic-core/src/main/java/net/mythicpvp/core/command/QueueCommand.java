package net.mythicpvp.core.command;

import net.mythicpvp.core.transfer.TransferQueueService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("queue")
@CommandPermission("mythic.core.queue.admin")
public final class QueueCommand extends MythicCommand {

    private final TransferQueueService queueService;

    public QueueCommand(@NotNull TransferQueueService queueService) {
        this.queueService = queueService;
    }

    @Default
    public void status(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize(
                "&#D2D8E0Queue: &#FFFFFFsize=" + queueService.size()
                        + "&#D2D8E0 paused=&#FFFFFF" + queueService.paused()
                        + "&#D2D8E0 enabled=&#FFFFFF" + queueService.enabled()
                        + "&#D2D8E0 drain/sec=&#FFFFFF" + queueService.drainPerTick()));
        sender.sendMessage(MythicHex.colorize("&7Type &#FFFFFF/queue help &7for the full subcommand list."));
    }

    @Subcommand("help")
    public void help(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#F529BEQueue Commands"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue &7- show queue size, pause state, drain rate"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue pause &7- stop draining (new transfers still queue up)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue resume &7- resume draining"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue enable &7- turn the queue back on"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue disable &7- bypass the queue entirely (clears it)"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue skip &7- transfer the next player immediately"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue clear &7- drop everyone from the queue"));
        sender.sendMessage(MythicHex.colorize("&#FFFFFF/queue rate <n> &7- set drain rate per second"));
    }

    @Subcommand("pause")
    public void pause(@NotNull CommandSender sender) {
        queueService.setPaused(true);
        sender.sendMessage(MythicHex.colorize("&#FFEC8AQueue paused. New transfers stop draining."));
    }

    @Subcommand("resume")
    public void resume(@NotNull CommandSender sender) {
        queueService.setPaused(false);
        sender.sendMessage(MythicHex.colorize("&#9CFF9CQueue resumed."));
    }

    @Subcommand("disable")
    public void disable(@NotNull CommandSender sender) {
        queueService.setEnabled(false);
        sender.sendMessage(MythicHex.colorize("&#FF8A8AQueue disabled. Transfers go through immediately, queue cleared."));
    }

    @Subcommand("enable")
    public void enable(@NotNull CommandSender sender) {
        queueService.setEnabled(true);
        sender.sendMessage(MythicHex.colorize("&#9CFF9CQueue enabled."));
    }

    @Subcommand("skip")
    public void skip(@NotNull CommandSender sender) {
        if (queueService.skipNext()) {
            sender.sendMessage(MythicHex.colorize("&#9CFF9CSkipped to next transfer."));
        } else {
            sender.sendMessage(MythicHex.colorize("&#FFEC8AQueue is empty."));
        }
    }

    @Subcommand("clear")
    public void clear(@NotNull CommandSender sender) {
        queueService.clear();
        sender.sendMessage(MythicHex.colorize("&#FF8A8AQueue cleared."));
    }

    @Subcommand("rate")
    public void rate(@NotNull CommandSender sender, int perSecond) {
        queueService.setDrainPerTick(perSecond);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CQueue drain rate set to &#FFFFFF" + queueService.drainPerTick() + "/sec&#9CFF9C."));
    }
}

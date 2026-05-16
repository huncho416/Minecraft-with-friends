package net.mythicpvp.core.command;

import net.mythicpvp.core.chat.ChatControlService;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Optional;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandAlias("unmute")
@CommandPermission("mythic.core.chat.mute")
public final class UnmuteCommand extends MythicCommand {

    private final ChatControlService chatControl;
    private final CoreMessages messages;

    public UnmuteCommand(@NotNull ChatControlService chatControl, @NotNull CoreMessages messages) {
        this.chatControl = chatControl;
        this.messages = messages;
    }

    @Default
    @Complete({"chat-scopes"})
    public void execute(@NotNull CommandSender sender, @Optional String scopeArg) {
        chatControl.unmute(ChatCommand.parseScope(scopeArg));
        sender.sendMessage(messages.component(
                "messages.chat-control.unmuted",
                "&#9CFF9CChat has been unmuted."));
    }
}

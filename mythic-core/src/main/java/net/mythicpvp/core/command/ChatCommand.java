package net.mythicpvp.core.command;

import net.mythicpvp.core.chat.ChatControlService;
import net.mythicpvp.core.chat.ChatScope;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Optional;
import net.mythicpvp.suite.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

@CommandAlias("chat")
@CommandPermission("mythic.core.chat")
public final class ChatCommand extends MythicCommand {

    private final ChatControlService chatControl;
    private final CoreMessages messages;

    public ChatCommand(@NotNull ChatControlService chatControl, @NotNull CoreMessages messages) {
        this.chatControl = chatControl;
        this.messages = messages;
    }

    @Default
    public void usage(@NotNull CommandSender sender) {
        sender.sendMessage(messages.component(
                "messages.chat-control.usage",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8AUsage: &#FFFFFF/chat <mute|unmute|slow|clear|status> [seconds] [local|network]"));
    }

    @Subcommand("mute")
    @CommandPermission("mythic.core.chat.mute")
    @Complete({"chat-scopes"})
    public void mute(@NotNull CommandSender sender, @Optional String scopeArg) {
        chatControl.mute(parseScope(scopeArg));
        sender.sendMessage(messages.component(
                "messages.chat-control.muted",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CChat has been muted."));
    }

    @Subcommand("unmute")
    @CommandPermission("mythic.core.chat.mute")
    @Complete({"chat-scopes"})
    public void unmute(@NotNull CommandSender sender, @Optional String scopeArg) {
        chatControl.unmute(parseScope(scopeArg));
        sender.sendMessage(messages.component(
                "messages.chat-control.unmuted",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CChat has been unmuted."));
    }

    @Subcommand("slow")
    @CommandPermission("mythic.core.chat.slow")
    @Complete({"chat-slow-presets", "chat-scopes"})
    public void slow(@NotNull CommandSender sender, int seconds, @Optional String scopeArg) {
        if (seconds < 0) {
            sender.sendMessage(messages.component(
                    "messages.chat-control.usage",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8AUsage: &#FFFFFF/chat <mute|unmute|slow|clear|status> [seconds] [local|network]"));
            return;
        }
        chatControl.slow(seconds, parseScope(scopeArg));
        if (seconds == 0) {
            sender.sendMessage(messages.component(
                    "messages.chat-control.slowed-off",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CChat slow mode is off."));
        } else {
            sender.sendMessage(messages.component(
                    "messages.chat-control.slowed",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#9CFF9CChat slow mode is now &#FFFFFF%seconds%s&#9CFF9C.",
                    Map.of("seconds", Integer.toString(seconds))));
        }
    }

    @Subcommand("clear")
    @CommandPermission("mythic.core.chat.clear")
    @Complete({"chat-scopes"})
    public void clear(@NotNull CommandSender sender, @Optional String scopeArg) {
        chatControl.clear(parseScope(scopeArg));

    }

    @Subcommand("status")
    public void status(@NotNull CommandSender sender) {
        sender.sendMessage(messages.component(
                "messages.chat-control.status",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FFFFFFmuted=%muted% slow=%seconds%s scope=%scope%",
                Map.of(
                        "muted", Boolean.toString(chatControl.muted()),
                        "seconds", Integer.toString(chatControl.slowSeconds()),
                        "scope", chatControl.state().scope().name().toLowerCase(Locale.ROOT))));
    }

    @NotNull
    static ChatScope parseScope(String arg) {
        if (arg == null || arg.isBlank()) {
            return ChatScope.LOCAL;
        }
        String normalized = arg.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "network", "net", "n", "global", "all" -> ChatScope.NETWORK;
            default -> ChatScope.LOCAL;
        };
    }
}

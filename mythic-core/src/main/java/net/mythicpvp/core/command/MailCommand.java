package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.social.MailMessage;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CommandAlias("mail")
public final class MailCommand extends MythicCommand {

    private final SocialService social;
    private final CoreMessages messages;

    public MailCommand(@NotNull SocialService social, @NotNull CoreMessages messages) {
        this.social = social;
        this.messages = messages;
    }

    @Default
    public void usage(@NotNull Player player) {
        player.sendMessage(messages.component(
                "messages.social.mail-usage",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AUsage: &#FFFFFF/mail <send|inbox|read>"));
    }

    @Subcommand("send")
    @Complete({"players"})
    public void send(@NotNull Player player, @NotNull String targetName, @NotNull String[] words) {
        if (words.length == 0) {
            usage(player);
            return;
        }
        UUID target = uuidForName(targetName);
        String body = String.join(" ", words);
        social.sendMail(player.getUniqueId(), target, "Message from " + player.getName(), body);
        player.sendMessage(messages.component(
                "messages.social.mail-sent",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CMail sent to &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", targetName)));
    }

    @Subcommand("inbox")
    public void inbox(@NotNull Player player) {
        List<MailMessage> inbox = social.inbox(player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.mail-inbox",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFFInbox: &#D2D8E0%count% &#FFFFFFmessages, &#D2D8E0%unread% &#FFFFFFunread.",
                Map.of(
                        "count", Integer.toString(inbox.size()),
                        "unread", Integer.toString(social.unread(player.getUniqueId()).size()))));
        inbox.stream().limit(5).forEach(mail -> player.sendMessage(messages.component(
                "messages.social.mail-entry",
                "&#D2D8E0#%id% &#FFFFFF%subject%",
                Map.of("id", Long.toString(mail.id()), "subject", mail.subject()))));
    }

    @Subcommand("read")
    public void read(@NotNull Player player, long mailId) {
        MailMessage mail = social.inbox(player.getUniqueId()).stream()
                .filter(message -> message.id() == mailId)
                .findFirst()
                .orElse(null);
        if (mail == null) {
            player.sendMessage(messages.component(
                    "messages.social.mail-missing",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AMail not found."));
            return;
        }
        social.markMailRead(mailId, player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.mail-read",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFF%body%",
                Map.of("body", mail.body())));
    }

    private UUID uuidForName(@NotNull String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.getUniqueId();
    }
}

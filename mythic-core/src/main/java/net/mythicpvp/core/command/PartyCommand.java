package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.social.Party;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@CommandAlias("party")
public final class PartyCommand extends MythicCommand {

    private final SocialService social;
    private final CoreMessages messages;

    public PartyCommand(@NotNull SocialService social, @NotNull CoreMessages messages) {
        this.social = social;
        this.messages = messages;
    }

    @Default
    public void usage(@NotNull Player player) {
        player.sendMessage(messages.component(
                "messages.social.party-usage",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AUsage: &#FFFFFF/party <create|invite|join|leave|disband|info|chat>"));
    }

    @Subcommand("create")
    public void create(@NotNull Player player) {
        Party party = social.createParty(player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.party-created",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CParty created. Party id: &#FFFFFF%id%&#9CFF9C.",
                Map.of("id", Long.toString(party.id()))));
    }

    @Subcommand("invite")
    @Complete({"players"})
    public void invite(@NotNull Player player, @NotNull String targetName) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null || !party.leader().equals(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-leader-only",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AYou must lead a party to do that."));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.command.player-not-found",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AThat player is not online."));
            return;
        }
        target.sendMessage(messages.component(
                "messages.social.party-invite",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFF%sender% &#9CFF9Cinvited you to party &#FFFFFF%id%&#9CFF9C. Use &#FFFFFF/party join %id%&#9CFF9C.",
                Map.of("sender", player.getName(), "id", Long.toString(party.id()))));
        player.sendMessage(messages.component(
                "messages.social.party-invite-sent",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CParty invite sent to &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", target.getName())));
    }

    @Subcommand("join")
    public void join(@NotNull Player player, long partyId) {
        if (!social.joinParty(partyId, player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-join-failed",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AUnable to join that party."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-joined",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CJoined party &#FFFFFF%id%&#9CFF9C.",
                Map.of("id", Long.toString(partyId))));
    }

    @Subcommand("leave")
    public void leave(@NotNull Player player) {
        if (!social.leaveParty(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-none",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AYou are not in a party."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-left",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CYou left the party."));
    }

    @Subcommand("disband")
    public void disband(@NotNull Player player) {
        if (!social.disbandParty(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-leader-only",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AYou must lead a party to do that."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-disbanded",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#9CFF9CParty disbanded."));
    }

    @Subcommand("info")
    public void info(@NotNull Player player) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(messages.component(
                    "messages.social.party-none",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AYou are not in a party."));
            return;
        }
        Set<UUID> members = social.membersOf(party.id());
        player.sendMessage(messages.component(
                "messages.social.party-info",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FFFFFFParty &#D2D8E0%id% &#FFFFFFhas &#D2D8E0%count% &#FFFFFFmembers.",
                Map.of("id", Long.toString(party.id()), "count", Integer.toString(members.size()))));
        for (UUID memberUuid : members) {
            Player online = Bukkit.getPlayer(memberUuid);
            String name = online != null ? online.getName() : memberUuid.toString().substring(0, 8);
            boolean isLeader = memberUuid.equals(party.leader());
            String role = isLeader ? "&#FFD700Leader" : "&#D2D8E0Member";
            player.sendMessage(messages.component(
                    "messages.social.party-member-entry",
                    "&#D2D8E0 - &#FFFFFF%name% %role%",
                    Map.of("name", name, "role", role)));
        }
    }

    @Subcommand("chat")
    public void chat(@NotNull Player player, @NotNull String[] words) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(messages.component(
                    "messages.social.party-none",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AYou are not in a party."));
            return;
        }
        if (words.length == 0) {
            player.sendMessage(messages.component(
                    "messages.social.party-chat-usage",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8\u00BB &#FF8A8AUsage: &#FFFFFF/party chat <message>"));
            return;
        }
        String message = String.join(" ", words);
        Set<UUID> members = social.membersOf(party.id());
        for (UUID memberUuid : members) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(messages.component(
                        "messages.social.party-chat",
                        "&#D2D8E0[Party] &#FFFFFF%sender% &8\u00BB &#D2D8E0%message%",
                        Map.of("sender", player.getName(), "message", message)));
            }
        }
    }
}

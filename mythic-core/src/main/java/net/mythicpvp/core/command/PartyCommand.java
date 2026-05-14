package net.mythicpvp.core.command;

import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.social.Party;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AUsage: &#FFFFFF/party <create|join|leave|disband|info>"));
    }

    @Subcommand("create")
    public void create(@NotNull Player player) {
        Party party = social.createParty(player.getUniqueId());
        player.sendMessage(messages.component(
                "messages.social.party-created",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#9CFF9CParty created. Party id: &#FFFFFF%id%&#9CFF9C.",
                Map.of("id", Long.toString(party.id()))));
    }

    @Subcommand("invite")
    public void invite(@NotNull Player player, @NotNull String targetName) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null || !party.leader().equals(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-leader-only",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AYou must lead a party to do that."));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.command.player-not-found",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AThat player is not online."));
            return;
        }
        target.sendMessage(messages.component(
                "messages.social.party-invite",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FFFFFF%sender% &#9CFF9Cinvited you to party &#FFFFFF%id%&#9CFF9C. Use &#FFFFFF/party join %id%&#9CFF9C.",
                Map.of("sender", player.getName(), "id", Long.toString(party.id()))));
        player.sendMessage(messages.component(
                "messages.social.party-invite-sent",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#9CFF9CParty invite sent to &#FFFFFF%target%&#9CFF9C.",
                Map.of("target", target.getName())));
    }

    @Subcommand("join")
    public void join(@NotNull Player player, long partyId) {
        if (!social.joinParty(partyId, player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-join-failed",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AUnable to join that party."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-joined",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#9CFF9CJoined party &#FFFFFF%id%&#9CFF9C.",
                Map.of("id", Long.toString(partyId))));
    }

    @Subcommand("leave")
    public void leave(@NotNull Player player) {
        if (!social.leaveParty(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-none",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AYou are not in a party."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-left",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#9CFF9CYou left the party."));
    }

    @Subcommand("disband")
    public void disband(@NotNull Player player) {
        if (!social.disbandParty(player.getUniqueId())) {
            player.sendMessage(messages.component(
                    "messages.social.party-leader-only",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AYou must lead a party to do that."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-disbanded",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#9CFF9CParty disbanded."));
    }

    @Subcommand("info")
    public void info(@NotNull Player player) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(messages.component(
                    "messages.social.party-none",
                    "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FF8A8AYou are not in a party."));
            return;
        }
        player.sendMessage(messages.component(
                "messages.social.party-info",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FFFFFFParty &#D2D8E0%id% &#FFFFFFhas &#D2D8E0%count% &#FFFFFFmembers.",
                Map.of("id", Long.toString(party.id()), "count", Integer.toString(social.membersOf(party.id()).size()))));
    }
}

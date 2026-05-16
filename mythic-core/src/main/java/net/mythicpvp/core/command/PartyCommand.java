package net.mythicpvp.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.mythicpvp.core.social.Party;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.core.transfer.ShardRegistry;
import net.mythicpvp.core.transfer.TransferQueueService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

@CommandAlias("party|p")
public final class PartyCommand extends MythicCommand {

    private final SocialService social;
    private final String localShardId;

    public PartyCommand(@NotNull SocialService social, @NotNull String localShardId) {
        this.social = social;
        this.localShardId = localShardId;
    }

    @Default
    public void usage(@NotNull Player player) {
        player.sendMessage(MythicHex.colorize("&#F529BEParty Commands"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party create &7- start a new party with you as leader"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party invite <player> &7- invite a player to your party"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party join <leader> &7- join a player's party by their username"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party leave &7- leave your current party"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party disband &7- disband the party (leader only)"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party info &7- show your party's members and roles"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party warp &7- summon every party member to your shard (leader only)"));
        player.sendMessage(MythicHex.colorize("&#FFFFFF/party chat <message> &7- send a message in party chat"));
    }

    @Subcommand("create")
    public void create(@NotNull Player player) {
        if (social.partyOf(player.getUniqueId()) != null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou are already in a party. Use &#FFFFFF/party leave &#FF8A8Afirst."));
            return;
        }
        social.createParty(player.getUniqueId());
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CParty created. Invite players with &#FFFFFF/party invite <player>&#9CFF9C."));
    }

    @Subcommand("invite")
    @Complete({"players"})
    public void invite(@NotNull Player player, @NotNull String targetName) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            social.createParty(player.getUniqueId());
            party = social.partyOf(player.getUniqueId());
            player.sendMessage(MythicHex.colorize(
                    "&#9CFF9CCreated a new party so you could invite players."));
        }
        if (party == null || !party.leader().equals(player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AOnly the party leader can invite."));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AThat player is not online."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou cannot invite yourself."));
            return;
        }
        String leaderName = player.getName();
        String joinCmd = "/party join " + leaderName;
        Component invite = MythicHex.colorize("&#FFFFFF" + leaderName
                + " &#9CFF9Cinvited you to their party. &#FFFFFF[Click to join]")
                .clickEvent(ClickEvent.runCommand(joinCmd))
                .hoverEvent(HoverEvent.showText(MythicHex.colorize(
                        "&#9CFF9CClick to run &#FFFFFF" + joinCmd)));
        target.sendMessage(invite);
        target.sendMessage(MythicHex.colorize(
                "&7(Or type &f" + joinCmd + " &7to join.)"));
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CParty invite sent to &#FFFFFF" + target.getName() + "&#9CFF9C."));
    }

    @Subcommand("join")
    @Complete({"players"})
    public void join(@NotNull Player player, @NotNull String leaderName) {
        OfflinePlayer leader = Bukkit.getOfflinePlayer(leaderName);
        UUID leaderUuid = leader.getUniqueId();
        Party party = social.partyLedBy(leaderUuid);
        if (party == null) {
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8A" + leaderName + " &#FF8A8Adoesn't lead a party right now."));
            return;
        }
        if (!social.joinParty(party.id(), player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize(
                    "&#FF8A8ACould not join — you may already be in a party."));
            return;
        }
        String resolvedLeader = leader.getName() == null ? leaderName : leader.getName();
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CJoined &#FFFFFF" + resolvedLeader + "&#9CFF9C's party."));
        notifyParty(party, "&#FFFFFF" + player.getName() + " &#9CFF9Cjoined the party.", player.getUniqueId());
    }

    @Subcommand("leave")
    public void leave(@NotNull Player player) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou are not in a party."));
            return;
        }
        boolean wasLeader = party.leader().equals(player.getUniqueId());
        social.leaveParty(player.getUniqueId());
        player.sendMessage(MythicHex.colorize("&#9CFF9CYou left the party."));
        if (wasLeader) {
            notifyParty(party, "&#FF8A8AThe party leader left. The party has been disbanded.", player.getUniqueId());
        } else {
            notifyParty(party, "&#FFFFFF" + player.getName() + " &#FFEC8Aleft the party.", player.getUniqueId());
        }
    }

    @Subcommand("disband")
    public void disband(@NotNull Player player) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null || !party.leader().equals(player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou must lead a party to do that."));
            return;
        }
        Set<UUID> members = social.membersOf(party.id());
        social.disbandParty(player.getUniqueId());
        for (UUID memberUuid : members) {
            if (memberUuid.equals(player.getUniqueId())) continue;
            Player m = Bukkit.getPlayer(memberUuid);
            if (m != null && m.isOnline()) {
                m.sendMessage(MythicHex.colorize(
                        "&#FF8A8A" + player.getName() + " &#FF8A8Adisbanded the party."));
            }
        }
        player.sendMessage(MythicHex.colorize("&#9CFF9CParty disbanded."));
    }

    @Subcommand("info")
    public void info(@NotNull Player player) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou are not in a party."));
            return;
        }
        Set<UUID> members = social.membersOf(party.id());
        String leaderName = nameOf(party.leader());
        player.sendMessage(MythicHex.colorize(
                "&#F529BE" + leaderName + "&#FFFFFF's party &7(&f" + members.size() + " &7members)"));
        for (UUID memberUuid : members) {
            String name = nameOf(memberUuid);
            boolean isLeader = memberUuid.equals(party.leader());
            String role = isLeader ? "&#FFD700Leader" : "&#D2D8E0Member";
            player.sendMessage(MythicHex.colorize("&8• &#FFFFFF" + name + " " + role));
        }
    }

    @Subcommand("warp")
    public void warp(@NotNull Player player) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null || !party.leader().equals(player.getUniqueId())) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AOnly the party leader can warp."));
            return;
        }
        TransferQueueService queue = lookupQueue();
        Set<UUID> members = social.membersOf(party.id());
        int warped = 0;
        for (UUID memberUuid : members) {
            if (memberUuid.equals(player.getUniqueId())) continue;
            Player m = Bukkit.getPlayer(memberUuid);
            if (m == null || !m.isOnline()) continue;
            if (queue != null) {
                queue.enqueue(m, localShardId);
            } else {
                m.transfer(localShardId, 25565);
            }
            m.sendMessage(MythicHex.colorize(
                    "&#9CFF9C" + player.getName() + " &#9CFF9Cis warping the party to &#FFFFFF" + localShardId + "&#9CFF9C."));
            warped++;
        }
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CWarped &#FFFFFF" + warped + " &#9CFF9Cparty member(s) to this shard."));
    }

    @Subcommand("chat")
    public void chat(@NotNull Player player, @NotNull String[] words) {
        Party party = social.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AYou are not in a party."));
            return;
        }
        if (words.length == 0) {
            player.sendMessage(MythicHex.colorize("&#FF8A8AUsage: &#FFFFFF/party chat <message>"));
            return;
        }
        String message = String.join(" ", words);
        Set<UUID> members = social.membersOf(party.id());
        Component formatted = MythicHex.colorize(
                "&#D2D8E0[Party] &#FFFFFF" + player.getName() + " &8» &#D2D8E0" + message);
        for (UUID memberUuid : members) {
            Player m = Bukkit.getPlayer(memberUuid);
            if (m != null && m.isOnline()) {
                m.sendMessage(formatted);
            }
        }
    }

    private void notifyParty(@NotNull Party party, @NotNull String message, @Nullable UUID exclude) {
        Component component = MythicHex.colorize(message);
        for (UUID memberUuid : social.membersOf(party.id())) {
            if (exclude != null && memberUuid.equals(exclude)) continue;
            Player m = Bukkit.getPlayer(memberUuid);
            if (m != null && m.isOnline()) {
                m.sendMessage(component);
            }
        }
    }

    @NotNull
    private static String nameOf(@NotNull UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        String fallback = Bukkit.getOfflinePlayer(uuid).getName();
        return fallback == null ? uuid.toString().substring(0, 8) : fallback;
    }

    @Nullable
    private static TransferQueueService lookupQueue() {
        RegisteredServiceProvider<TransferQueueService> rsp =
                Bukkit.getServicesManager().getRegistration(TransferQueueService.class);
        return rsp == null ? null : rsp.getProvider();
    }

    @SuppressWarnings("unused")
    @Nullable
    private static ShardRegistry lookupShardRegistry() {
        RegisteredServiceProvider<ShardRegistry> rsp =
                Bukkit.getServicesManager().getRegistration(ShardRegistry.class);
        return rsp == null ? null : rsp.getProvider();
    }
}

package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.social.FriendLink;
import net.mythicpvp.core.social.FriendRequest;
import net.mythicpvp.core.social.MailMessage;
import net.mythicpvp.core.social.Party;
import net.mythicpvp.core.social.PartyMember;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public final class MainThreadHydrationSink implements HydrationSink {

    private final JavaPlugin plugin;
    private final HydrationSink inner;

    public MainThreadHydrationSink(@NotNull JavaPlugin plugin, @NotNull HydrationSink inner) {
        this.plugin = plugin;
        this.inner = inner;
    }

    @Override public void applyRank(@NotNull CoreRank rank) { schedule(s -> s.applyRank(rank)); }
    @Override public void removeRank(@NotNull String rankId) { schedule(s -> s.removeRank(rankId)); }
    @Override public void applyGrant(@NotNull RankGrant grant) { schedule(s -> s.applyGrant(grant)); }
    @Override public void removeGrant(long grantId) { schedule(s -> s.removeGrant(grantId)); }
    @Override public void applyPunishment(@NotNull PunishmentRecord record) { schedule(s -> s.applyPunishment(record)); }
    @Override public void removePunishment(long punishmentId) { schedule(s -> s.removePunishment(punishmentId)); }
    @Override public void applyTemplate(@NotNull PunishmentTemplate template) { schedule(s -> s.applyTemplate(template)); }
    @Override public void removeTemplate(@NotNull String title) { schedule(s -> s.removeTemplate(title)); }
    @Override public void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active) {
        schedule(s -> s.applyBlacklist(target, targetName, active));
    }
    @Override public void applyFriend(@NotNull FriendLink friend) { schedule(s -> s.applyFriend(friend)); }
    @Override public void removeFriend(long friendId) { schedule(s -> s.removeFriend(friendId)); }
    @Override public void applyFriendRequest(@NotNull FriendRequest request) { schedule(s -> s.applyFriendRequest(request)); }
    @Override public void removeFriendRequest(long requestId) { schedule(s -> s.removeFriendRequest(requestId)); }
    @Override public void applyParty(@NotNull Party party) { schedule(s -> s.applyParty(party)); }
    @Override public void removeParty(long partyId) { schedule(s -> s.removeParty(partyId)); }
    @Override public void applyPartyMember(@NotNull PartyMember member) { schedule(s -> s.applyPartyMember(member)); }
    @Override public void removePartyMember(long memberId) { schedule(s -> s.removePartyMember(memberId)); }
    @Override public void applyMail(@NotNull MailMessage mail) { schedule(s -> s.applyMail(mail)); }
    @Override public void removeMail(long mailId) { schedule(s -> s.removeMail(mailId)); }

    private void schedule(@NotNull Consumer<HydrationSink> action) {

        if (plugin.getServer().isPrimaryThread()) {
            action.accept(inner);
            return;
        }

        MythicScheduler.runSync(plugin, () -> action.accept(inner));
    }
}

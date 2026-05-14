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
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface HydrationSink {

    void applyRank(@NotNull CoreRank rank);

    void removeRank(@NotNull String rankId);

    void applyGrant(@NotNull RankGrant grant);

    void removeGrant(long grantId);

    void applyPunishment(@NotNull PunishmentRecord record);

    void removePunishment(long punishmentId);

    void applyTemplate(@NotNull PunishmentTemplate template);

    void removeTemplate(@NotNull String title);

    void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active);

    void applyFriend(@NotNull FriendLink friend);

    void removeFriend(long friendId);

    void applyFriendRequest(@NotNull FriendRequest request);

    void removeFriendRequest(long requestId);

    void applyParty(@NotNull Party party);

    void removeParty(long partyId);

    void applyPartyMember(@NotNull PartyMember member);

    void removePartyMember(long memberId);

    void applyMail(@NotNull MailMessage mail);

    void removeMail(long mailId);
}

package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.social.FriendLink;
import net.mythicpvp.core.social.FriendRequest;
import net.mythicpvp.core.social.MailMessage;
import net.mythicpvp.core.social.Party;
import net.mythicpvp.core.social.PartyMember;
import net.mythicpvp.core.social.SocialService;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public final class CoreHydrationSink implements HydrationSink {

    private final Logger logger;
    private final RankService rankService;
    private final GrantService grantService;
    private final PunishmentService punishmentService;
    private final SocialService socialService;
    private final ConcurrentMap<UUID, String> blacklist = new ConcurrentHashMap<>();

    public CoreHydrationSink(
            @NotNull Logger logger,
            @NotNull RankService rankService,
            @NotNull GrantService grantService,
            @NotNull PunishmentService punishmentService,
            @NotNull SocialService socialService) {
        this.logger = logger;
        this.rankService = rankService;
        this.grantService = grantService;
        this.punishmentService = punishmentService;
        this.socialService = socialService;
    }

    @Override public void applyRank(@NotNull CoreRank rank) { rankService.applyRank(rank); }
    @Override public void removeRank(@NotNull String rankId) { rankService.removeRank(rankId); }

    @Override public void applyGrant(@NotNull RankGrant grant) { grantService.applyGrant(grant); }
    @Override public void removeGrant(long grantId) { grantService.removeGrant(grantId); }

    @Override public void applyPunishment(@NotNull PunishmentRecord record) { punishmentService.applyRecord(record); }
    @Override public void removePunishment(long punishmentId) { punishmentService.removeRecord(punishmentId); }

    @Override public void applyTemplate(@NotNull PunishmentTemplate template) { punishmentService.applyTemplateRow(template); }
    @Override public void removeTemplate(@NotNull String title) { punishmentService.removeTemplateRow(title); }

    @Override
    public void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active) {
        if (active) {
            blacklist.put(target, targetName);
        } else {
            blacklist.remove(target);
        }
    }

    @Override public void applyFriend(@NotNull FriendLink friend) { socialService.applyFriend(friend); }
    @Override public void removeFriend(long friendId) { socialService.removeFriend(friendId); }
    @Override public void applyFriendRequest(@NotNull FriendRequest request) { socialService.applyFriendRequest(request); }
    @Override public void removeFriendRequest(long requestId) { socialService.removeFriendRequest(requestId); }
    @Override public void applyParty(@NotNull Party party) { socialService.applyParty(party); }
    @Override public void removeParty(long partyId) { socialService.removeParty(partyId); }
    @Override public void applyPartyMember(@NotNull PartyMember member) { socialService.applyPartyMember(member); }
    @Override public void removePartyMember(long memberId) { socialService.removePartyMember(memberId); }
    @Override public void applyMail(@NotNull MailMessage mail) { socialService.applyMail(mail); }
    @Override public void removeMail(long mailId) { socialService.removeMail(mailId); }

    @NotNull
    public java.util.Set<UUID> blacklistedUuids() {
        return java.util.Set.copyOf(blacklist.keySet());
    }

    public boolean isBlacklisted(@NotNull UUID target) {
        return blacklist.containsKey(target);
    }

    @SuppressWarnings("unused")
    private void logSwallowed(@NotNull String op, @NotNull Throwable error) {
        logger.warning("[hydration] " + op + " failed: " + error.getMessage());
    }
}

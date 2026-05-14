package net.mythicpvp.core.social;

import net.mythicpvp.core.persistence.PersistenceGateway;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SocialService {

    private final Clock clock;
    private final PersistenceGateway persistence;
    private final AtomicLong localIds = new AtomicLong(-1L);
    private final ConcurrentMap<Long, FriendRequest> friendRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, FriendLink>> friends = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Party> parties = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ConcurrentMap<UUID, PartyMember>> partyMembers = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> partyByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MailMessage> mail = new ConcurrentHashMap<>();

    public SocialService(@NotNull PersistenceGateway persistence, @NotNull Clock clock) {
        this.persistence = persistence;
        this.clock = clock;
    }

    public FriendRequest requestFriend(@NotNull UUID from, @NotNull UUID to) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("cannot friend yourself");
        }
        FriendRequest existing = incomingRequests(to).stream()
                .filter(request -> request.from().equals(from))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        FriendRequest request = new FriendRequest(nextLocalId(), from, to, clock.millis());
        applyFriendRequest(request);
        persistence.friendRequest(from, to);
        return request;
    }

    public boolean acceptFriend(long requestId, @NotNull UUID acceptingPlayer) {
        FriendRequest request = friendRequests.get(requestId);
        if (request == null || !request.to().equals(acceptingPlayer)) {
            return false;
        }
        removeFriendRequest(requestId);
        long now = clock.millis();
        applyFriend(new FriendLink(nextLocalId(), request.from(), request.to(), now));
        applyFriend(new FriendLink(nextLocalId(), request.to(), request.from(), now));
        persistence.friendAccept(requestId);
        return true;
    }

    public void removeFriend(@NotNull UUID owner, @NotNull UUID friend) {
        removeFriendLink(owner, friend);
        removeFriendLink(friend, owner);
        persistence.friendRemove(owner, friend);
    }

    public boolean areFriends(@NotNull UUID first, @NotNull UUID second) {
        return friends.getOrDefault(first, new ConcurrentHashMap<>()).containsKey(second);
    }

    @NotNull
    public Set<UUID> friendsOf(@NotNull UUID owner) {
        return Set.copyOf(friends.getOrDefault(owner, new ConcurrentHashMap<>()).keySet());
    }

    @NotNull
    public List<FriendRequest> incomingRequests(@NotNull UUID target) {
        return friendRequests.values().stream()
                .filter(request -> request.to().equals(target))
                .sorted(Comparator.comparingLong(FriendRequest::createdAtMillis))
                .toList();
    }

    public Party createParty(@NotNull UUID leader) {
        Long existing = partyByPlayer.get(leader);
        if (existing != null) {
            Party party = parties.get(existing);
            if (party != null) {
                return party;
            }
        }
        long id = nextLocalId();
        Party party = new Party(id, leader, clock.millis());
        applyParty(party);
        applyPartyMember(new PartyMember(nextLocalId(), id, leader, clock.millis()));
        persistence.partyCreate(leader);
        return party;
    }

    public boolean joinParty(long partyId, @NotNull UUID player) {
        if (!parties.containsKey(partyId) || partyByPlayer.containsKey(player)) {
            return false;
        }
        applyPartyMember(new PartyMember(nextLocalId(), partyId, player, clock.millis()));
        persistence.partyJoin(partyId, player);
        return true;
    }

    public boolean leaveParty(@NotNull UUID player) {
        Long partyId = partyByPlayer.get(player);
        if (partyId == null) {
            return false;
        }
        Party party = parties.get(partyId);
        removePartyMember(partyId, player);
        if (party != null && party.leader().equals(player)) {
            removeParty(partyId);
        }
        persistence.partyLeave(partyId, player);
        return true;
    }

    public boolean disbandParty(@NotNull UUID leader) {
        Long partyId = partyByPlayer.get(leader);
        if (partyId == null) {
            return false;
        }
        Party party = parties.get(partyId);
        if (party == null || !party.leader().equals(leader)) {
            return false;
        }
        removeParty(partyId);
        persistence.partyDisband(partyId);
        return true;
    }

    public Party partyOf(@NotNull UUID player) {
        Long partyId = partyByPlayer.get(player);
        return partyId == null ? null : parties.get(partyId);
    }

    @NotNull
    public Set<UUID> membersOf(long partyId) {
        return Set.copyOf(partyMembers.getOrDefault(partyId, new ConcurrentHashMap<>()).keySet());
    }

    public MailMessage sendMail(
            @NotNull UUID sender,
            @NotNull UUID recipient,
            @NotNull String subject,
            @NotNull String body) {
        MailMessage message = new MailMessage(
                nextLocalId(), recipient, sender, subject, body, "[]", false, clock.millis(), 0L);
        applyMail(message);
        persistence.mailSend(sender, recipient, subject, body, "[]");
        return message;
    }

    public boolean markMailRead(long mailId, @NotNull UUID recipient) {
        MailMessage message = mail.get(mailId);
        if (message == null || !message.recipient().equals(recipient)) {
            return false;
        }
        applyMail(message.markRead(clock.millis()));
        persistence.mailMarkRead(mailId);
        return true;
    }

    @NotNull
    public List<MailMessage> inbox(@NotNull UUID recipient) {
        return mail.values().stream()
                .filter(message -> message.recipient().equals(recipient))
                .sorted(Comparator.comparingLong(MailMessage::sentAtMillis).reversed())
                .toList();
    }

    @NotNull
    public List<MailMessage> unread(@NotNull UUID recipient) {
        return inbox(recipient).stream().filter(message -> !message.read()).toList();
    }

    public void applyFriend(@NotNull FriendLink link) {
        friends.computeIfAbsent(link.owner(), ignored -> new ConcurrentHashMap<>())
                .put(link.friend(), link);
    }

    public void removeFriend(long id) {
        for (Map<UUID, FriendLink> links : friends.values()) {
            links.values().removeIf(link -> link.id() == id);
        }
    }

    public void applyFriendRequest(@NotNull FriendRequest request) {
        friendRequests.put(request.id(), request);
    }

    public void removeFriendRequest(long requestId) {
        friendRequests.remove(requestId);
    }

    public void applyParty(@NotNull Party party) {
        parties.put(party.id(), party);
    }

    public void removeParty(long partyId) {
        parties.remove(partyId);
        ConcurrentMap<UUID, PartyMember> removedMembers = partyMembers.remove(partyId);
        if (removedMembers != null) {
            removedMembers.keySet().forEach(partyByPlayer::remove);
        }
    }

    public void applyPartyMember(@NotNull PartyMember member) {
        partyMembers.computeIfAbsent(member.partyId(), ignored -> new ConcurrentHashMap<>())
                .put(member.player(), member);
        partyByPlayer.put(member.player(), member.partyId());
    }

    public void removePartyMember(long memberId) {
        for (Map.Entry<Long, ConcurrentMap<UUID, PartyMember>> entry : partyMembers.entrySet()) {
            UUID removed = null;
            for (Map.Entry<UUID, PartyMember> memberEntry : entry.getValue().entrySet()) {
                if (memberEntry.getValue().id() == memberId) {
                    removed = memberEntry.getKey();
                    break;
                }
            }
            if (removed != null) {
                removePartyMember(entry.getKey(), removed);
                return;
            }
        }
    }

    public void applyMail(@NotNull MailMessage message) {
        mail.put(message.id(), message);
    }

    public void removeMail(long mailId) {
        mail.remove(mailId);
    }

    private void removeFriendLink(@NotNull UUID owner, @NotNull UUID friend) {
        ConcurrentMap<UUID, FriendLink> links = friends.get(owner);
        if (links != null) {
            links.remove(friend);
        }
    }

    private void removePartyMember(long partyId, @NotNull UUID player) {
        ConcurrentMap<UUID, PartyMember> members = partyMembers.get(partyId);
        if (members != null) {
            members.remove(player);
            if (members.isEmpty()) {
                partyMembers.remove(partyId);
            }
        }
        partyByPlayer.remove(player);
    }

    private long nextLocalId() {
        return localIds.getAndDecrement();
    }
}

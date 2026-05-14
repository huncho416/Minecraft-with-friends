package net.mythicpvp.core.social;

import net.mythicpvp.core.persistence.CapturingPersistenceGateway;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID ALEX = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BLAKE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void friendRequestAcceptAndRemoveUpdatesLocalStateAndGateway() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        SocialService social = new SocialService(gateway, CLOCK);

        FriendRequest request = social.requestFriend(ALEX, BLAKE);
        assertEquals(1, social.incomingRequests(BLAKE).size());
        assertTrue(gateway.calls.get(0) instanceof CapturingPersistenceGateway.FriendRequestCall);

        assertTrue(social.acceptFriend(request.id(), BLAKE));
        assertTrue(social.areFriends(ALEX, BLAKE));
        assertTrue(social.areFriends(BLAKE, ALEX));

        social.removeFriend(ALEX, BLAKE);
        assertFalse(social.areFriends(ALEX, BLAKE));
        assertFalse(social.areFriends(BLAKE, ALEX));
    }

    @Test
    void partyLifecycleTracksLeaderAndMembers() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        SocialService social = new SocialService(gateway, CLOCK);

        Party party = social.createParty(ALEX);
        assertEquals(ALEX, party.leader());
        assertEquals(1, social.membersOf(party.id()).size());

        assertTrue(social.joinParty(party.id(), BLAKE));
        assertEquals(2, social.membersOf(party.id()).size());

        assertTrue(social.leaveParty(BLAKE));
        assertEquals(1, social.membersOf(party.id()).size());

        assertTrue(social.disbandParty(ALEX));
        assertEquals(0, social.membersOf(party.id()).size());
    }

    @Test
    void mailSendInboxAndReadState() {
        CapturingPersistenceGateway gateway = new CapturingPersistenceGateway();
        SocialService social = new SocialService(gateway, CLOCK);

        MailMessage message = social.sendMail(ALEX, BLAKE, "Subject", "Body");
        assertEquals(1, social.inbox(BLAKE).size());
        assertEquals(1, social.unread(BLAKE).size());

        assertTrue(social.markMailRead(message.id(), BLAKE));
        assertEquals(0, social.unread(BLAKE).size());
        assertTrue(gateway.calls.stream().anyMatch(CapturingPersistenceGateway.MailMarkRead.class::isInstance));
    }
}

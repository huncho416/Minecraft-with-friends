package net.mythicpvp.core.announce;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pure-fn tests for the broadcast wire shape — keeps the
 * `core:broadcast` payload format pinned so a refactor can't
 * silently drop the {@code origin} field that prevents echo loops.
 */
class BroadcastNoticeTest {

    @Test
    void noticeRetainsMessageAndOrigin() {
        BroadcastNotice notice = new BroadcastNotice("hello world", "hub-1");
        assertEquals("hello world", notice.message());
        assertEquals("hub-1", notice.origin());
    }

    @Test
    void recordEqualityHonorsBothFields() {
        // Records auto-generate equals — pin it so a future migration
        // away from records can't quietly drop the contract.
        BroadcastNotice a = new BroadcastNotice("hi", "hub-1");
        BroadcastNotice b = new BroadcastNotice("hi", "hub-1");
        BroadcastNotice c = new BroadcastNotice("hi", "sb-1");
        assertEquals(a, b);
        assertNotEquals(a, c, "different origin must compare unequal");
    }
}

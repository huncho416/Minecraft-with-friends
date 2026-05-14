package net.mythicpvp.core.announce;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BroadcastNoticeTest {

    @Test
    void noticeRetainsMessageAndOrigin() {
        BroadcastNotice notice = new BroadcastNotice("hello world", "hub-1");
        assertEquals("hello world", notice.message());
        assertEquals("hub-1", notice.origin());
    }

    @Test
    void recordEqualityHonorsBothFields() {

        BroadcastNotice a = new BroadcastNotice("hi", "hub-1");
        BroadcastNotice b = new BroadcastNotice("hi", "hub-1");
        BroadcastNotice c = new BroadcastNotice("hi", "sb-1");
        assertEquals(a, b);
        assertNotEquals(a, c, "different origin must compare unequal");
    }
}

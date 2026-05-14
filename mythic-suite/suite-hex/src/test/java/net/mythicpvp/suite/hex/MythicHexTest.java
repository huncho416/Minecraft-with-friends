package net.mythicpvp.suite.hex;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MythicHexTest {

    @Test
    void parsesHexAndPreservesText() {
        assertEquals("Mythic", PlainTextComponentSerializer.plainText().serialize(MythicHex.colorize("&#FF00F8Mythic")));
        assertArrayEquals(new int[]{255, 0, 248}, MythicHex.fromHex("#FF00F8"));
        assertEquals("#FF00F8", MythicHex.toHexString(255, 0, 248));
    }

    @Test
    void appliesFontKey() {
        assertEquals("mythic:font/title", MythicHex.font("mythic:font/title", "Title").font().asString());
    }
}

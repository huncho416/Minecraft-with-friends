package net.mythicpvp.core.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuotedArgsTest {

    @Test
    void emptyInputYieldsEmptyList() {
        assertTrue(QuotedArgs.parse("").isEmpty());
        assertTrue(QuotedArgs.parse(new String[0]).isEmpty());
    }

    @Test
    void unquotedArgsSplitOnSpaces() {
        assertEquals(List.of("a", "b", "c"), QuotedArgs.parse("a b c"));
        assertEquals(List.of("a", "b"), QuotedArgs.parse("  a   b  "));
    }

    @Test
    void quotedSegmentBecomesSingleArg() {
        assertEquals(
                List.of("MUTE", "1d", "Chat Offense", "first", "chat", "offense"),
                QuotedArgs.parse("MUTE 1d \"Chat Offense\" first chat offense"));
    }

    @Test
    void quotedSegmentCanContainPunctuation() {

        assertEquals(
                List.of("Chat Offense | first offense"),
                QuotedArgs.parse("\"Chat Offense | first offense\""));
    }

    @Test
    void unmatchedQuoteSwallowsRestOfLine() {

        assertEquals(
                List.of("oops no closing quote here"),
                QuotedArgs.parse("\"oops no closing quote here"));
    }

    @Test
    void arrayInputJoinsAndReparses() {

        String[] words = { "MUTE", "1d", "\"Chat", "Offense", "#1\"", "more", "info" };
        assertEquals(
                List.of("MUTE", "1d", "Chat Offense #1", "more", "info"),
                QuotedArgs.parse(words));
    }

    @Test
    void mixedQuotedAndUnquoted() {
        assertEquals(
                List.of("alpha", "two words here", "beta"),
                QuotedArgs.parse("alpha \"two words here\" beta"));
    }

    @Test
    void emptyQuotedSegmentEmitsEmptyArg() {

        assertEquals(List.of("a", "b"), QuotedArgs.parse("a \"\" b"));
    }
}

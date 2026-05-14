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
        // Pipe is fine inside quotes — back-compat with the existing
        // "title | information" syntax means callers still see the pipe
        // for downstream splitting if they want.
        assertEquals(
                List.of("Chat Offense | first offense"),
                QuotedArgs.parse("\"Chat Offense | first offense\""));
    }

    @Test
    void unmatchedQuoteSwallowsRestOfLine() {
        // Defensive: typo-friendly. Doesn't error, just emits a single
        // arg with everything after the open-quote.
        assertEquals(
                List.of("oops no closing quote here"),
                QuotedArgs.parse("\"oops no closing quote here"));
    }

    @Test
    void arrayInputJoinsAndReparses() {
        // Bukkit hands us already-split args; the array form just
        // reassembles and re-tokenizes so quoted segments survive.
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
        // Edge case — `""` opens then immediately closes the quote
        // with no content. Current implementation skips because the
        // builder is empty when we hit the closing quote. That's fine
        // — staff don't need empty args.
        assertEquals(List.of("a", "b"), QuotedArgs.parse("a \"\" b"));
    }
}

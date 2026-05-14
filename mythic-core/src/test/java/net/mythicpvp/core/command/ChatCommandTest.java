package net.mythicpvp.core.command;

import net.mythicpvp.core.chat.ChatScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-fn tests for the scope-arg parser used by {@code /chat <sub> [scope]}.
 *
 * <p>The default-to-LOCAL behaviour is a safety policy — a typo or
 * forgotten arg shouldn't accidentally mute the entire network. Tests
 * pin every shorthand we accept so future refactors don't quietly drop
 * one.
 */
class ChatCommandTest {

    @Test
    void nullOrBlankDefaultsToLocal() {
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope(null));
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope(""));
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("   "));
    }

    @Test
    void networkSynonymsAllParse() {
        for (String input : new String[] { "network", "Network", "NETWORK", "net", "n", "global", "all" }) {
            assertEquals(ChatScope.NETWORK, ChatCommand.parseScope(input),
                    "expected NETWORK for input " + input);
        }
    }

    @Test
    void unknownArgFallsBackToLocal() {
        // Tolerant rather than rejecting — a typo defaults to the safe scope.
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("worldwide"));
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("local"));
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("here"));
    }
}

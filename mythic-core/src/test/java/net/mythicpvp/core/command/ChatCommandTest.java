package net.mythicpvp.core.command;

import net.mythicpvp.core.chat.ChatScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("worldwide"));
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("local"));
        assertEquals(ChatScope.LOCAL, ChatCommand.parseScope("here"));
    }
}

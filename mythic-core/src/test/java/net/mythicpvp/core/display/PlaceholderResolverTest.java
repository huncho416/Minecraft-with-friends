package net.mythicpvp.core.display;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderResolverTest {

    @Test
    void resolvesKnownPlaceholders() {
        String result = new PlaceholderResolver()
                .set("rank", "Owner")
                .set("player", "Notch")
                .resolve("&c[%rank%] %player%");
        assertEquals("&c[Owner] Notch", result);
    }

    @Test
    void preservesUnknownPlaceholdersVerbatim() {

        String result = new PlaceholderResolver()
                .set("rank", "Owner")
                .resolve("%rank% %papi_relation_friends%");
        assertEquals("Owner %papi_relation_friends%", result);
    }

    @Test
    void caseInsensitiveLookups() {

        PlaceholderResolver r = new PlaceholderResolver().set("RANK", "Owner");
        assertEquals("Owner", r.get("rank"));
        assertEquals("Owner", r.get("RANK"));
    }

    @Test
    void emptyAndNoTokenStringsShortCircuit() {
        PlaceholderResolver r = new PlaceholderResolver().set("rank", "X");
        assertEquals("", r.resolve(""));
        assertEquals("plain text no tokens", r.resolve("plain text no tokens"));
        assertEquals("100% off", r.resolve("100% off"),
                "stray % without a closing % is preserved (not greedy)");
    }

    @Test
    void resolveAllProcessesEachLine() {
        PlaceholderResolver r = new PlaceholderResolver()
                .set("rank", "VIP")
                .set("server", "hub-1");
        List<String> result = r.resolveAll(List.of(
                "&aRank: %rank%",
                "&aServer: %server%",
                "&aDiscord: discord.gg/mythic"));
        assertEquals(3, result.size());
        assertEquals("&aRank: VIP", result.get(0));
        assertEquals("&aServer: hub-1", result.get(1));
        assertEquals("&aDiscord: discord.gg/mythic", result.get(2));
    }

    @Test
    void placeholderValueWithSpecialReplaceCharsIsLiteral() {

        String result = new PlaceholderResolver()
                .set("color", "&#FF00F8")
                .resolve("%color%MythicPvP");
        assertEquals("&#FF00F8MythicPvP", result);
    }

    @Test
    void multipleOccurrencesAllResolve() {
        String result = new PlaceholderResolver()
                .set("server", "hub-1")
                .resolve("On %server%, welcome to %server%");
        assertEquals("On hub-1, welcome to hub-1", result);
    }
}

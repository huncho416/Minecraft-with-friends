package net.mythicpvp.core.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny shell-style argument splitter for command surfaces that accept
 * multi-word values.
 *
 * <p>Supports double-quoted segments — {@code "Chat Offense #1"} parses
 * as a single arg with the spaces preserved. Backslash-escapes are NOT
 * supported (no real Minecraft player will type them); a literal {@code "}
 * inside a quoted segment can't be expressed. That's a deliberate
 * trade-off for parser simplicity.
 *
 * <p>Existing pipe-delimited usage (e.g. {@code title | information})
 * still works because pipes aren't special — they end up inside one of
 * the parsed args and the call site can split them itself.
 *
 * <p>Pure function. Unit-tested in {@code QuotedArgsTest}.
 */
public final class QuotedArgs {

    private QuotedArgs() {}

    /**
     * Re-tokenize an already-split {@code String[]} (Bukkit hands us
     * one of these per command) by re-joining and re-splitting on
     * spaces, honoring quoted segments. Quotes can span multiple
     * input array entries.
     */
    @NotNull
    public static List<String> parse(@NotNull String[] words) {
        if (words.length == 0) {
            return List.of();
        }
        return parse(String.join(" ", words));
    }

    /**
     * Tokenize a raw line. Quoted segments collapse spaces; unmatched
     * quotes consume to end of line and emit a single arg with the
     * tail content (no error — staff can fix the typo and try again).
     */
    @NotNull
    public static List<String> parse(@NotNull String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }
}

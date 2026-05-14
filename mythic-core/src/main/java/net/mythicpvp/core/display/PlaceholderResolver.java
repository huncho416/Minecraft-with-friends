package net.mythicpvp.core.display;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny `%placeholder%` resolver for tablist / scoreboard / nametag
 * templates.
 *
 * <p>Deliberately small — we don't pull in PlaceholderAPI as a hard
 * dependency. Templates use the conventional `%name%` token shape so a
 * future PAPI bridge is a drop-in: it just needs to expand the same
 * tokens via PAPI's hooks.
 *
 * <p>Unknown placeholders are left as-is rather than blanked. That
 * makes mistakes visible in-game (you see `%typo%` rather than empty
 * space) and lets PAPI later resolve tokens we don't know about
 * natively.
 *
 * <p>Pure function — no Bukkit state. Trivial to unit-test.
 */
public final class PlaceholderResolver {

    /** Standard placeholder shape — `%token%` where token is `[a-z0-9_]+`. */
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-z0-9_]+)%");

    private final Map<String, String> values = new HashMap<>();

    /**
     * Set a placeholder value. Keys are case-folded to lower so callers
     * can pass {@code "Player"} or {@code "player"} interchangeably.
     */
    @NotNull
    public PlaceholderResolver set(@NotNull String key, @NotNull String value) {
        values.put(key.toLowerCase(), value);
        return this;
    }

    /** Read a value (mostly for tests). */
    public String get(@NotNull String key) {
        return values.get(key.toLowerCase());
    }

    /**
     * Resolve every {@code %placeholder%} in {@code template} against the
     * registered values. Unknown tokens are preserved verbatim — future
     * resolvers (PAPI, suite-hex) can have a second pass over the result.
     */
    @NotNull
    public String resolve(@NotNull String template) {
        if (template.isEmpty() || template.indexOf('%') < 0) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder(template.length());
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = values.get(key);
            // Append literal $/\ in the replacement — Matcher's appendReplacement
            // treats both as escape characters and would otherwise double-process
            // user-supplied colors that legitimately contain those bytes.
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(replacement == null ? matcher.group(0) : replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Convenience for resolving a list of templates. */
    @NotNull
    public java.util.List<String> resolveAll(@NotNull java.util.List<String> templates) {
        java.util.List<String> out = new java.util.ArrayList<>(templates.size());
        for (String template : templates) {
            out.add(resolve(template));
        }
        return out;
    }
}

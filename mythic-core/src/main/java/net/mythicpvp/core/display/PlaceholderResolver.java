package net.mythicpvp.core.display;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-z0-9_]+)%");

    private final Map<String, String> values = new HashMap<>();

    @NotNull
    public PlaceholderResolver set(@NotNull String key, @NotNull String value) {
        values.put(key.toLowerCase(), value);
        return this;
    }

    public String get(@NotNull String key) {
        return values.get(key.toLowerCase());
    }

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

            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(replacement == null ? matcher.group(0) : replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @NotNull
    public java.util.List<String> resolveAll(@NotNull java.util.List<String> templates) {
        java.util.List<String> out = new java.util.ArrayList<>(templates.size());
        for (String template : templates) {
            out.add(resolve(template));
        }
        return out;
    }
}

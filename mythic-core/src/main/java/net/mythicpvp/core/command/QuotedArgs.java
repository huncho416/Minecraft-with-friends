package net.mythicpvp.core.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class QuotedArgs {

    private QuotedArgs() {}

    @NotNull
    public static List<String> parse(@NotNull String[] words) {
        if (words.length == 0) {
            return List.of();
        }
        return parse(String.join(" ", words));
    }

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

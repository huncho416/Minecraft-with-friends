package net.mythicpvp.core.chat;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class ChatFilterEntry {

    public enum Type {
        LITERAL,
        REGEX
    }

    private final long id;
    private String title;
    private Type type;
    private final List<String> patterns = new ArrayList<>();
    private boolean autoPunish;
    private volatile List<Pattern> compiled = List.of();

    public ChatFilterEntry(long id, @NotNull String title, @NotNull Type type, @NotNull List<String> patterns, boolean autoPunish) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.patterns.addAll(patterns);
        this.autoPunish = autoPunish;
        recompile();
    }

    public long id() { return id; }
    @NotNull public String title() { return title; }
    @NotNull public Type type() { return type; }
    @NotNull public List<String> patterns() { return List.copyOf(patterns); }
    public boolean autoPunish() { return autoPunish; }

    public void setTitle(@NotNull String title) { this.title = title; }

    public void setType(@NotNull Type type) {
        this.type = type;
        recompile();
    }

    public void setPatterns(@NotNull List<String> patterns) {
        this.patterns.clear();
        this.patterns.addAll(patterns);
        recompile();
    }

    public void addPattern(@NotNull String pattern) {
        this.patterns.add(pattern);
        recompile();
    }

    public boolean removePattern(@NotNull String pattern) {
        boolean removed = this.patterns.remove(pattern);
        if (removed) recompile();
        return removed;
    }

    public boolean removePatternAt(int index) {
        if (index < 0 || index >= patterns.size()) return false;
        patterns.remove(index);
        recompile();
        return true;
    }

    public void setAutoPunish(boolean autoPunish) { this.autoPunish = autoPunish; }

    public boolean matches(@NotNull String text) {
        for (Pattern p : compiled) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private void recompile() {
        List<Pattern> next = new ArrayList<>();
        for (String raw : patterns) {
            if (raw == null || raw.isBlank()) continue;
            try {
                String regex = type == Type.REGEX ? raw : literalToBypassResistantRegex(raw);
                next.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            } catch (RuntimeException ignored) {
            }
        }
        this.compiled = List.copyOf(next);
    }

    @NotNull
    static String literalToBypassResistantRegex(@NotNull String literal) {
        StringBuilder sb = new StringBuilder("(?i)");
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (Character.isLetter(c)) {
                sb.append("[").append(Character.toLowerCase(c)).append(Character.toUpperCase(c)).append("]");
                sb.append("[\\s\\W_0-9]*");
            } else if (Character.isDigit(c)) {
                sb.append(c).append("[\\s\\W_]*");
            }
        }
        if (sb.length() > 4) {
            sb.setLength(sb.length() - "[\\s\\W_0-9]*".length());
        }
        return sb.toString();
    }

    @NotNull
    static List<String> splitPatterns(@NotNull String input) {
        return Arrays.stream(input.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

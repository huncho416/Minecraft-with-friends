package net.mythicpvp.core.chat;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class ChatFilterEntry {

    public enum Type {
        LITERAL,
        REGEX
    }

    private final long id;
    private String title;
    private Type type;
    private String pattern;
    private boolean autoPunish;
    private volatile Pattern compiled;

    public ChatFilterEntry(long id, @NotNull String title, @NotNull Type type, @NotNull String pattern, boolean autoPunish) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.pattern = pattern;
        this.autoPunish = autoPunish;
        recompile();
    }

    public long id() { return id; }
    @NotNull public String title() { return title; }
    @NotNull public Type type() { return type; }
    @NotNull public String pattern() { return pattern; }
    public boolean autoPunish() { return autoPunish; }

    public void setTitle(@NotNull String title) { this.title = title; }

    public void setType(@NotNull Type type) {
        this.type = type;
        recompile();
    }

    public void setPattern(@NotNull String pattern) {
        this.pattern = pattern;
        recompile();
    }

    public void setAutoPunish(boolean autoPunish) { this.autoPunish = autoPunish; }

    public boolean matches(@NotNull String text) {
        if (compiled == null) return false;
        return compiled.matcher(text).find();
    }

    private void recompile() {
        try {
            String regex = type == Type.REGEX ? pattern : literalToBypassResistantRegex(pattern);
            this.compiled = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (RuntimeException e) {
            this.compiled = null;
        }
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
}

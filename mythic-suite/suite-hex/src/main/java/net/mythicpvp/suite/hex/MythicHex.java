package net.mythicpvp.suite.hex;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MythicHex {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static final Map<String, String> LEGACY_COLOR_MAP = new HashMap<>();

    static {
        LEGACY_COLOR_MAP.put("&0", "<black>");
        LEGACY_COLOR_MAP.put("&1", "<dark_blue>");
        LEGACY_COLOR_MAP.put("&2", "<dark_green>");
        LEGACY_COLOR_MAP.put("&3", "<dark_aqua>");
        LEGACY_COLOR_MAP.put("&4", "<dark_red>");
        LEGACY_COLOR_MAP.put("&5", "<dark_purple>");
        LEGACY_COLOR_MAP.put("&6", "<gold>");
        LEGACY_COLOR_MAP.put("&7", "<gray>");
        LEGACY_COLOR_MAP.put("&8", "<dark_gray>");
        LEGACY_COLOR_MAP.put("&9", "<blue>");
        LEGACY_COLOR_MAP.put("&a", "<green>");
        LEGACY_COLOR_MAP.put("&b", "<aqua>");
        LEGACY_COLOR_MAP.put("&c", "<red>");
        LEGACY_COLOR_MAP.put("&d", "<light_purple>");
        LEGACY_COLOR_MAP.put("&e", "<yellow>");
        LEGACY_COLOR_MAP.put("&f", "<white>");
        LEGACY_COLOR_MAP.put("&l", "<bold>");
        LEGACY_COLOR_MAP.put("&m", "<strikethrough>");
        LEGACY_COLOR_MAP.put("&n", "<underlined>");
        LEGACY_COLOR_MAP.put("&o", "<italic>");
        LEGACY_COLOR_MAP.put("&k", "<obfuscated>");
        LEGACY_COLOR_MAP.put("&r", "<reset>");
    }

    private MythicHex() {}

    @NotNull
    public static Component colorize(@NotNull String text) {
        return MINI_MESSAGE.deserialize(toMiniMessage(text));
    }

    @NotNull
    public static Component colorize(@NotNull String text, @NotNull Map<String, String> placeholders) {
        String processed = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            processed = processed.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return colorize(processed);
    }

    @NotNull
    public static Component gradient(@NotNull String startHex, @NotNull String endHex, @NotNull String text) {
        return MINI_MESSAGE.deserialize("<gradient:" + startHex + ":" + endHex + ">" + text + "</gradient>");
    }

    @NotNull
    public static Component gradient(@NotNull String startHex, @NotNull String midHex, @NotNull String endHex, @NotNull String text) {
        return MINI_MESSAGE.deserialize("<gradient:" + startHex + ":" + midHex + ":" + endHex + ">" + text + "</gradient>");
    }

    @NotNull
    public static Component mythicGradient(@NotNull String text) {
        return gradient("#FF00F8", "#FF9FFC", "#FFFFFF", text);
    }

    @NotNull
    public static Component font(@NotNull String fontKey, @NotNull String text) {
        Component component = colorize(text);
        if (fontKey.isBlank()) {
            return component;
        }
        return component.font(net.kyori.adventure.key.Key.key(fontKey));
    }

    @NotNull
    public static String toLegacy(@NotNull Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    @NotNull
    public static String stripColor(@NotNull String text) {
        String stripped = HEX_PATTERN.matcher(text).replaceAll("");
        for (String code : LEGACY_COLOR_MAP.keySet()) {
            stripped = stripped.replace(code, "");
        }
        return stripped;
    }

    @NotNull
    public static TextColor parseHex(@NotNull String hex) {
        String clean = hex.startsWith("#") ? hex : "#" + hex;
        TextColor color = TextColor.fromHexString(clean);
        if (color == null) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        return color;
    }

    @NotNull
    public static String toMiniMessage(@NotNull String text) {
        String result = text;

        Matcher hexMatcher = HEX_PATTERN.matcher(result);
        StringBuilder hexBuilder = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuilder, "<color:#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(hexBuilder);
        result = hexBuilder.toString();

        for (Map.Entry<String, String> entry : LEGACY_COLOR_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @NotNull
    public static String toHexString(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    @NotNull
    public static int[] fromHex(@NotNull String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{
                Integer.parseInt(clean.substring(0, 2), 16),
                Integer.parseInt(clean.substring(2, 4), 16),
                Integer.parseInt(clean.substring(4, 6), 16)
        };
    }
}

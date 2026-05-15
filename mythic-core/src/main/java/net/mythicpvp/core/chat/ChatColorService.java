package net.mythicpvp.core.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatColorService {

    private final Map<UUID, String> active = new ConcurrentHashMap<>();
    private final Map<String, ChatColorOption> options = new LinkedHashMap<>();

    public ChatColorService() {
        register("white", "&f", "WHITE_DYE");
        register("light_gray", "&7", "LIGHT_GRAY_DYE");
        register("gray", "&8", "GRAY_DYE");
        register("yellow", "&e", "YELLOW_DYE");
        register("gold", "&6", "ORANGE_DYE");
        register("red", "&c", "RED_DYE");
        register("dark_red", "&4", "REDSTONE");
        register("pink", "&d", "PINK_DYE");
        register("purple", "&5", "PURPLE_DYE");
        register("blue", "&9", "BLUE_DYE");
        register("dark_blue", "&1", "LAPIS_LAZULI");
        register("aqua", "&b", "LIGHT_BLUE_DYE");
        register("dark_aqua", "&3", "CYAN_DYE");
        register("green", "&a", "LIME_DYE");
        register("dark_green", "&2", "GREEN_DYE");
        register("black", "&0", "INK_SAC");
    }

    private void register(@NotNull String key, @NotNull String code, @NotNull String dye) {
        options.put(key, new ChatColorOption(key, code, dye));
    }

    @NotNull
    public List<ChatColorOption> options() {
        return List.copyOf(options.values());
    }

    @Nullable
    public ChatColorOption byKey(@NotNull String key) {
        return options.get(key.toLowerCase(Locale.ROOT));
    }

    @Nullable
    public String colorFor(@NotNull UUID player) {
        return active.get(player);
    }

    public void setColor(@NotNull UUID player, @NotNull String code) {
        active.put(player, code);
    }

    public void clear(@NotNull UUID player) {
        active.remove(player);
    }

    @Nullable
    public static String permissionKeyFor(@NotNull String code) {
        return switch (code) {
            case "&f" -> "white";
            case "&7" -> "light_gray";
            case "&8" -> "gray";
            case "&e" -> "yellow";
            case "&6" -> "gold";
            case "&c" -> "red";
            case "&4" -> "dark_red";
            case "&d" -> "pink";
            case "&5" -> "purple";
            case "&9" -> "blue";
            case "&1" -> "dark_blue";
            case "&b" -> "aqua";
            case "&3" -> "dark_aqua";
            case "&a" -> "green";
            case "&2" -> "dark_green";
            case "&0" -> "black";
            default -> null;
        };
    }

    public record ChatColorOption(@NotNull String key, @NotNull String code, @NotNull String dye) {

        @NotNull
        public String permission() {
            return ChatFormatListener.COLOR_PERMISSION_PREFIX + key;
        }

        @NotNull
        public String displayName() {
            String[] parts = key.split("_");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(' ');
                String p = parts[i];
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
            return sb.toString();
        }
    }
}

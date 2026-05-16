package net.mythicpvp.core.rank;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerNameColor {

    private static final String DEFAULT_COLOR = "<gray>";

    private final RankService rankService;
    private final GrantService grantService;

    public PlayerNameColor(@NotNull RankService rankService, @NotNull GrantService grantService) {
        this.rankService = rankService;
        this.grantService = grantService;
    }

    @NotNull
    public String colorize(@NotNull UUID uuid, @NotNull String name) {
        return colorTag(uuid) + name;
    }

    @NotNull
    public String colorTag(@NotNull UUID uuid) {
        try {
            String rankId = grantService.activeRank(uuid);
            CoreRank rank = rankService.get(rankId);
            if (rank == null) return DEFAULT_COLOR;
            return miniColor(rank.color());
        } catch (RuntimeException ignored) {
            return DEFAULT_COLOR;
        }
    }

    @NotNull
    public static String miniColor(@NotNull String raw) {
        if (raw.isBlank()) return DEFAULT_COLOR;
        String trimmed = raw.trim();
        if (trimmed.startsWith("&#") && trimmed.length() >= 9) {
            return "<" + trimmed.substring(1, 8) + ">";
        }
        if (trimmed.startsWith("#") && trimmed.length() >= 7) {
            return "<" + trimmed.substring(0, 7) + ">";
        }
        if (trimmed.startsWith("&") && trimmed.length() >= 2) {
            return legacyToTag(trimmed.charAt(1));
        }
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            return trimmed;
        }
        return DEFAULT_COLOR;
    }

    @NotNull
    private static String legacyToTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            default -> DEFAULT_COLOR;
        };
    }
}

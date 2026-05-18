package net.mythicpvp.core.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ChatFormatListener implements Listener {

    public static final String COLOR_PERMISSION_PREFIX = "mythic.core.color.";
    public static final String COLOR_WILDCARD = COLOR_PERMISSION_PREFIX + "*";

    private final RankService ranks;
    private final GrantService grants;
    private final MythicConfig coreConfig;
    private final ChatColorService chatColors;

    public ChatFormatListener(@NotNull RankService ranks,
                              @NotNull GrantService grants,
                              @NotNull MythicConfig coreConfig,
                              @NotNull ChatColorService chatColors) {
        this.ranks = ranks;
        this.grants = grants;
        this.coreConfig = coreConfig;
        this.chatColors = chatColors;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player sender = event.getPlayer();
        UUID uuid = sender.getUniqueId();
        CoreRank rank = activeRank(uuid);
        String template = defaultTemplate();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String chosenColor = chatColors == null ? null : chatColors.colorFor(uuid);
        String messageColor = effectiveMessageColor(sender, rank, chosenColor);
        String prefix = rankPrefix(rank);
        String playerColor = playerNameColor(rank);
        String cleanMessage = stripColorCodes(sanitize(message));
        String chatTagSegment = cosmeticTagSegment(uuid);
        String renderedMessage = cosmeticColorMessage(uuid, cleanMessage);
        if (renderedMessage == null) {
            renderedMessage = messageColor + cleanMessage;
        }
        String visibleName = DisguiseManager.getInstance().getDisplayName(uuid, sender.getName());
        String rendered = template
                .replace("%player%", chatTagSegment + prefix + playerColor + visibleName)
                .replace("%message%", renderedMessage)
                .replace("%chat_prefix%", sanitize(prefix))
                .replace("%chat_tag%", chatTagSegment)
                .replace("%rank%", rank == null ? "" : rank.name())
                .replace("%rank_color%", playerColor);
        Component component = MythicHex.colorize(rendered);
        event.renderer((source, displayName, msg, viewer) -> component);
    }

    @NotNull
    private String cosmeticTagSegment(@NotNull UUID uuid) {
        String format = equippedFormat(uuid, CosmeticType.CHAT_TAG);
        if (format == null || format.isBlank()) {
            return "";
        }
        return format + " ";
    }

    @Nullable
    private String cosmeticColorMessage(@NotNull UUID uuid, @NotNull String cleanMessage) {
        String format = equippedFormat(uuid, CosmeticType.CHAT_COLOR);
        if (format == null || format.isBlank()) {
            return null;
        }
        if (!format.contains("%message%")) {
            return format + cleanMessage;
        }
        return format.replace("%message%", cleanMessage);
    }

    @Nullable
    private String equippedFormat(@NotNull UUID uuid, @NotNull CosmeticType type) {
        CosmeticManager manager = CosmeticManager.getInstance();
        String equippedId = manager.getEquipped(uuid, type);
        if (equippedId == null) {
            return null;
        }
        if (!manager.ownsCosmetic(uuid, equippedId)) {
            return null;
        }
        CosmeticManager.Cosmetic cosmetic = manager.get(equippedId);
        if (cosmetic == null) {
            return null;
        }
        return cosmetic.format();
    }

    @NotNull
    private String defaultTemplate() {
        return coreConfig.getString(
                "chat.format.default",
                "&#D2D8E0%player% &8» &7%message%");
    }

    @NotNull
    private String rankPrefix(@Nullable CoreRank rank) {
        if (rank == null || rank.id().equalsIgnoreCase("default")) {
            return "";
        }
        String prefix = rank.chatPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = rank.prefix();
        }
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return MythicHex.normalizeBareHex(prefix) + " ";
    }

    @NotNull
    private String playerNameColor(@Nullable CoreRank rank) {
        if (rank == null || rank.id().equalsIgnoreCase("default")) {
            return "&#D2D8E0";
        }
        String color = rank.color();
        return color == null || color.isBlank() ? "&#D2D8E0" : ampHex(color);
    }

    @Nullable
    private CoreRank activeRank(@NotNull UUID uuid) {
        String rankId = grants.activeRank(uuid);
        CoreRank rank = ranks.get(rankId);
        if (rank != null) {
            return rank;
        }
        return ranks.get("default");
    }

    @NotNull
    private static String stripColorCodes(@NotNull String input) {
        return input
                .replaceAll("(?i)&#[0-9a-f]{6}", "")
                .replaceAll("(?i)&[0-9a-fk-or]", "")
                .replaceAll("(?i)§#[0-9a-f]{6}", "")
                .replaceAll("(?i)§[0-9a-fk-or]", "")
                .replaceAll("(?i)#[0-9a-f]{6}", "");
    }

    @NotNull
    private String effectiveMessageColor(@NotNull Player sender, @Nullable CoreRank rank, @Nullable String chosenColor) {
        if (chosenColor != null && !chosenColor.isBlank() && playerCanUseColor(sender, chosenColor)) {
            return chosenColor;
        }
        if (rank == null || rank.id().equalsIgnoreCase("default")) {
            return "&7";
        }
        String color = rank.color();
        if (color == null || color.isBlank()) {
            return "&7";
        }
        return ampHex(color);
    }

    private boolean playerCanUseColor(@NotNull Player sender, @NotNull String color) {
        if (sender.hasPermission(COLOR_WILDCARD)) {
            return true;
        }
        String key = ChatColorService.permissionKeyFor(color);
        return key != null && sender.hasPermission(COLOR_PERMISSION_PREFIX + key);
    }

    @NotNull
    private static String ampHex(@NotNull String input) {
        if (input.startsWith("#") && !input.startsWith("&#")) {
            return "&" + input;
        }
        return input;
    }

    @NotNull
    private static String sanitize(@NotNull String input) {
        return input
                .replace("%player%", "")
                .replace("%message%", "")
                .replace("%chat_prefix%", "");
    }

}

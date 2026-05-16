package net.mythicpvp.core.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.config.MythicConfig;
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
        String template = templateFor(rank);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String chosenColor = chatColors == null ? null : chatColors.colorFor(uuid);
        String messageColor = effectiveMessageColor(sender, rank, chosenColor);
        String rendered = template
                .replace("%player%", sender.getName())
                .replace("%message%", messageColor + sanitize(message))
                .replace("%chat_prefix%", rank == null ? "" : sanitize(rank.chatPrefix()))
                .replace("%rank%", rank == null ? "" : rank.name())
                .replace("%rank_color%", rank == null ? "&7" : ampHex(rank.color()));
        Component component = MythicHex.colorize(rendered);
        event.renderer((source, displayName, msg, viewer) -> component);
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
    private String templateFor(@Nullable CoreRank rank) {
        String defaultTemplate = coreConfig.getString(
                "chat.format.default",
                "&#D2D8E0%player% &8» &7%message%");
        if (rank == null) {
            return defaultTemplate;
        }
        if (rank.id().equalsIgnoreCase("default")) {
            return defaultTemplate;
        }
        String fromRank = rank.chatFormat();
        if (fromRank == null || fromRank.isBlank() || fromRank.contains("&f<%player%>")) {
            return defaultTemplate;
        }
        return fromRank;
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

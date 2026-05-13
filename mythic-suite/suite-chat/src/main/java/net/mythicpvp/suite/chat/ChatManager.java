package net.mythicpvp.suite.chat;

import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class ChatManager {

    private static final ChatManager INSTANCE = new ChatManager();
    private final Map<UUID, ChatChannel> playerChannels = new ConcurrentHashMap<>();
    private final List<Pattern> blockedPatterns = new ArrayList<>();
    private String chatFormat = "%prefix% %player%&7: &f%message%";

    private ChatManager() {
        blockedPatterns.add(Pattern.compile("(?i)(https?://|www\\.)\\S+"));
        blockedPatterns.add(Pattern.compile("(?i)\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"));
        blockedPatterns.add(Pattern.compile("(?i)\\b(?:[a-z0-9-]+\\.)+(?:com|net|org|gg|io)\\b"));
    }

    @NotNull
    public static ChatManager getInstance() {
        return INSTANCE;
    }

    public void setChatFormat(@NotNull String format) {
        this.chatFormat = format;
    }

    public void loadFormat(@NotNull ConfigText text) {
        this.chatFormat = text.raw("chat.format", "%prefix% %player%&7: &f%message%");
    }

    @NotNull
    public String getChatFormat() {
        return chatFormat;
    }

    @NotNull
    public ChatChannel getChannel(@NotNull UUID player) {
        return playerChannels.getOrDefault(player, ChatChannel.GLOBAL);
    }

    public void setChannel(@NotNull UUID player, @NotNull ChatChannel channel) {
        playerChannels.put(player, channel);
    }

    public void addBlockedPattern(@NotNull Pattern pattern) {
        blockedPatterns.add(pattern);
    }

    @NotNull
    public Component format(@NotNull Player player, @NotNull String prefix, @NotNull String message) {
        String visibleName = DisguiseManager.getInstance().getDisplayName(player.getUniqueId(), player.getName());
        String formatted = chatFormat
                .replace("%prefix%", prefix)
                .replace("%player%", visibleName)
                .replace("%message%", filter(message));
        return MythicHex.colorize(formatted);
    }

    @NotNull
    public String filter(@NotNull String message) {
        String filtered = message;
        for (Pattern pattern : blockedPatterns) {
            filtered = pattern.matcher(filtered).replaceAll("***");
        }
        return filtered;
    }

    public boolean isBlocked(@NotNull String message) {
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(message).find()) return true;
        }
        return false;
    }

    public void removePlayer(@NotNull UUID player) {
        playerChannels.remove(player);
    }
}

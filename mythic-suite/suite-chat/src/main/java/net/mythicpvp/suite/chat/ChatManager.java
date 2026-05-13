package net.mythicpvp.suite.chat;

import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class ChatManager {

    private static final ChatManager INSTANCE = new ChatManager();
    private final Map<UUID, ChatChannel> playerChannels = new ConcurrentHashMap<>();
    private final Map<UUID, MessageWindow> messageWindows = new ConcurrentHashMap<>();
    private final List<Pattern> blockedPatterns = new CopyOnWriteArrayList<>();
    private volatile String chatFormat = "%prefix% %player%&7: &f%message%";
    private volatile long spamWindowMillis = 3_000L;
    private volatile int spamMessageLimit = 4;

    private ChatManager() {
        resetDefaultBlockedPatterns();
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

    public void clearBlockedPatterns() {
        blockedPatterns.clear();
    }

    public void resetDefaultBlockedPatterns() {
        blockedPatterns.clear();
        blockedPatterns.add(Pattern.compile("(?i)(https?://|www\\.)\\S+"));
        blockedPatterns.add(Pattern.compile("(?i)\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"));
        blockedPatterns.add(Pattern.compile("(?i)\\b(?:[a-z0-9-]+\\.)+(?:com|net|org|gg|io)\\b"));
        blockedPatterns.add(Pattern.compile("(?i)\\b(?:kys|kill\\s+yourself)\\b"));
    }

    public void setSpamPolicy(long windowMillis, int messageLimit) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("Spam window must be positive");
        }
        if (messageLimit <= 0) {
            throw new IllegalArgumentException("Spam message limit must be positive");
        }
        this.spamWindowMillis = windowMillis;
        this.spamMessageLimit = messageLimit;
    }

    public boolean recordAndCheckSpam(@NotNull UUID player) {
        long now = System.currentTimeMillis();
        MessageWindow window = messageWindows.compute(player, (uuid, current) -> {
            if (current == null || now - current.startedAtMillis() > spamWindowMillis) {
                return new MessageWindow(now, 1);
            }
            return new MessageWindow(current.startedAtMillis(), current.count() + 1);
        });
        return window.count() > spamMessageLimit;
    }

    public boolean isSpam(@NotNull UUID player) {
        MessageWindow window = messageWindows.get(player);
        if (window == null) {
            return false;
        }
        if (System.currentTimeMillis() - window.startedAtMillis() > spamWindowMillis) {
            messageWindows.remove(player, window);
            return false;
        }
        return window.count() > spamMessageLimit;
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
        messageWindows.remove(player);
    }

    public void clear() {
        playerChannels.clear();
        messageWindows.clear();
        chatFormat = "%prefix% %player%&7: &f%message%";
        spamWindowMillis = 3_000L;
        spamMessageLimit = 4;
        resetDefaultBlockedPatterns();
    }

    private record MessageWindow(long startedAtMillis, int count) {}
}

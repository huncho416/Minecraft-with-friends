package net.mythicpvp.core.chat;

import net.mythicpvp.core.punishment.PunishmentRequest;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.suite.config.MythicConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ChatFilterService {

    private static final Duration[] ESCALATION_LADDER = new Duration[] {
            Duration.ofMinutes(15),
            Duration.ofHours(3),
            Duration.ofDays(1),
            Duration.ofDays(7),
            Duration.ofDays(30),
            Duration.ofDays(365),
    };

    private final MythicConfig config;
    private final PunishmentService punishments;
    private final Clock clock;
    private final String serverId;
    private final Map<Long, ChatFilterEntry> filters = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> offenseCounts = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ChatFilterService(@NotNull MythicConfig config,
                             @NotNull PunishmentService punishments,
                             @NotNull Clock clock,
                             @NotNull String serverId) {
        this.config = config;
        this.punishments = punishments;
        this.clock = clock;
        this.serverId = serverId;
    }

    public void load() {
        filters.clear();
        ConfigurationSection root = config.getConfig().getConfigurationSection("filters");
        if (root == null) return;
        long maxId = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection entry = root.getConfigurationSection(key);
            if (entry == null) continue;
            long id;
            try {
                id = Long.parseLong(key);
            } catch (NumberFormatException e) {
                continue;
            }
            String title = entry.getString("title", "Untitled");
            String typeStr = entry.getString("type", "LITERAL").toUpperCase(Locale.ROOT);
            ChatFilterEntry.Type type;
            try {
                type = ChatFilterEntry.Type.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                type = ChatFilterEntry.Type.LITERAL;
            }
            List<String> patterns = new ArrayList<>();
            if (entry.isList("patterns")) {
                for (String p : entry.getStringList("patterns")) {
                    if (p != null && !p.isBlank()) patterns.add(p);
                }
            }
            String legacy = entry.getString("pattern", "");
            if (!legacy.isBlank()) patterns.add(legacy);
            boolean autoPunish = entry.getBoolean("auto-punish", true);
            if (patterns.isEmpty()) continue;
            filters.put(id, new ChatFilterEntry(id, title, type, patterns, autoPunish));
            if (id > maxId) maxId = id;
        }
        nextId.set(maxId + 1);
    }

    public void save() {
        ConfigurationSection root = config.getConfig().createSection("filters");
        for (ChatFilterEntry entry : filters.values()) {
            ConfigurationSection section = root.createSection(Long.toString(entry.id()));
            section.set("title", entry.title());
            section.set("type", entry.type().name());
            section.set("patterns", entry.patterns());
            section.set("auto-punish", entry.autoPunish());
        }
        config.save();
    }

    @NotNull
    public ChatFilterEntry add(@NotNull String title, @NotNull ChatFilterEntry.Type type, @NotNull List<String> patterns, boolean autoPunish) {
        long id = nextId.getAndIncrement();
        ChatFilterEntry entry = new ChatFilterEntry(id, title, type, patterns, autoPunish);
        filters.put(id, entry);
        save();
        return entry;
    }

    public boolean remove(long id) {
        boolean removed = filters.remove(id) != null;
        if (removed) save();
        return removed;
    }

    @Nullable
    public ChatFilterEntry findByTitle(@NotNull String title) {
        String norm = title.trim().toLowerCase(Locale.ROOT);
        return filters.values().stream()
                .filter(e -> e.title().toLowerCase(Locale.ROOT).equals(norm))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public ChatFilterEntry get(long id) {
        return filters.get(id);
    }

    @NotNull
    public List<ChatFilterEntry> all() {
        List<ChatFilterEntry> out = new ArrayList<>(filters.values());
        out.sort(Comparator.comparingLong(ChatFilterEntry::id));
        return out;
    }

    @Nullable
    public ChatFilterEntry matchFirst(@NotNull String message) {
        for (ChatFilterEntry entry : filters.values()) {
            if (entry.matches(message)) {
                return entry;
            }
        }
        return null;
    }

    public int offenseCount(@NotNull UUID playerUuid) {
        return offenseCounts.getOrDefault(playerUuid, 0);
    }

    @NotNull
    public ChatFilterAction handleOffense(@NotNull UUID playerUuid,
                                          @NotNull String playerName,
                                          @NotNull ChatFilterEntry entry) {
        if (!entry.autoPunish()) {
            recordHistoryWarn(playerUuid, playerName, entry, "Filter category: " + entry.title());
            return ChatFilterAction.warnHistory(
                    "Your message was removed for using an inappropriate word.");
        }
        int offenses = offenseCounts.merge(playerUuid, 1, Integer::sum);
        if (offenses == 1) {
            recordHistoryWarn(playerUuid, playerName, entry, "Filter category: " + entry.title() + " (warning)");
            return ChatFilterAction.warnHistory(
                    "Your message was removed for using an inappropriate word. "
                            + "The next violation will result in a punishment.");
        }
        int muteIndex = Math.min(offenses - 2, ESCALATION_LADDER.length - 1);
        Duration duration = muteIndex >= 0 && muteIndex < ESCALATION_LADDER.length
                ? ESCALATION_LADDER[muteIndex]
                : null;
        if (offenses - 2 >= ESCALATION_LADDER.length) {
            issuePunishment(playerUuid, playerName, entry, null, true);
            return ChatFilterAction.permanentMute(
                    "You have been permanently muted for repeated use of inappropriate language.");
        }
        boolean atFinalRung = (offenses - 2) == ESCALATION_LADDER.length - 1;
        issuePunishment(playerUuid, playerName, entry, duration, false);
        String formatted = duration == null ? "permanent" : formatDuration(duration);
        if (atFinalRung) {
            return ChatFilterAction.mute(duration,
                    "You have been muted for " + formatted
                            + " for inappropriate language. "
                            + "The next violation will be a permanent mute.");
        }
        return ChatFilterAction.mute(duration,
                "You have been muted for " + formatted
                        + " for repeated use of inappropriate language.");
    }

    private void recordHistoryWarn(@NotNull UUID playerUuid,
                                   @NotNull String playerName,
                                   @NotNull ChatFilterEntry entry,
                                   @NotNull String reason) {
        try {
            punishments.punish(new PunishmentRequest(
                    playerUuid,
                    playerName,
                    PunishmentService.SYSTEM_STAFF,
                    "ChatFilter",
                    PunishmentType.WARN,
                    reason,
                    "",
                    null,
                    true,
                    false,
                    serverId));
        } catch (RuntimeException ignored) {
        }
    }

    private void issuePunishment(@NotNull UUID playerUuid,
                                 @NotNull String playerName,
                                 @NotNull ChatFilterEntry entry,
                                 @Nullable Duration duration,
                                 boolean permanent) {
        try {
            PunishmentType type = (duration == null || permanent) ? PunishmentType.MUTE : PunishmentType.TEMP_MUTE;
            punishments.punish(new PunishmentRequest(
                    playerUuid,
                    playerName,
                    PunishmentService.SYSTEM_STAFF,
                    "ChatFilter",
                    type,
                    "Filter category: " + entry.title(),
                    "",
                    duration == null ? null : clock.instant().plus(duration),
                    true,
                    false,
                    serverId));
        } catch (RuntimeException ignored) {
        }
    }

    @NotNull
    private static String formatDuration(@NotNull Duration duration) {
        long days = duration.toDays();
        if (days >= 1) return days + "d";
        long hours = duration.toHours();
        if (hours >= 1) return hours + "h";
        long minutes = duration.toMinutes();
        return minutes + "m";
    }
}

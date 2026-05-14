package net.mythicpvp.core.announce;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class BroadcastService {

    public static final String CHANNEL = "core:broadcast";

    private final ProtocolManager protocolManager;
    private final String shardId;
    private final AtomicInteger nextIndex = new AtomicInteger();
    private volatile List<String> announcementMessages = List.of();
    private volatile String broadcastFormat = "%message%";
    private volatile boolean enabled = true;
    private volatile int intervalSeconds = 300;

    public BroadcastService(@NotNull ProtocolManager protocolManager, @NotNull String shardId) {
        this.protocolManager = protocolManager;
        this.shardId = shardId;
        this.protocolManager.subscribe(CHANNEL,
                message -> receive(message.deserialize(BroadcastNotice.class)));
    }

    public void load(@NotNull MythicConfig config) {
        this.enabled = config.getBoolean("announcements.enabled", true);
        this.intervalSeconds = Math.max(5, config.getInt("announcements.interval-seconds", 300));
        this.announcementMessages = List.copyOf(config.getStringList("announcements.messages"));
        this.broadcastFormat = config.getString(
                "broadcast.format", "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8Â\u00BB &#FFFFFF%message%");

        nextIndex.set(0);
    }

    public boolean enabled() {
        return enabled;
    }

    public int intervalSeconds() {
        return intervalSeconds;
    }

    public int announcementCount() {
        return announcementMessages.size();
    }

    public void broadcast(@NotNull String rawMessage) {
        String formatted = applyFormat(rawMessage);
        publish(formatted);

        renderLocally(formatted);
    }

    public String tickAnnouncement() {
        if (!enabled || announcementMessages.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(nextIndex.getAndIncrement(), announcementMessages.size());
        String raw = announcementMessages.get(index);
        broadcast(raw);
        return raw;
    }

    private void publish(@NotNull String formatted) {
        protocolManager.publish(CHANNEL, new BroadcastNotice(formatted, shardId));
    }

    private void receive(@NotNull BroadcastNotice notice) {
        if (notice.origin().equals(shardId)) {

            return;
        }
        renderLocally(notice.message());
    }

    private void renderLocally(@NotNull String formatted) {
        Component component = MythicHex.colorize(formatted);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }

        Bukkit.getConsoleSender().sendMessage(component);
    }

    @NotNull
    private String applyFormat(@NotNull String rawMessage) {
        return interpolate(broadcastFormat, Map.of("message", rawMessage));
    }

    @NotNull
    private static String interpolate(@NotNull String template, @NotNull Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}

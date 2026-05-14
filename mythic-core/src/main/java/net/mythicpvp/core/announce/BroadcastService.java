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

/**
 * Owns network-wide broadcasts and the rotating-announcement loop.
 *
 * <p>Two responsibilities, one service:
 *
 * <ul>
 *   <li>{@code broadcast(line)} — fan a single line out to every player
 *       on every server. Uses the {@code core:broadcast} protocol
 *       channel; receivers render via {@link Bukkit#broadcast}.
 *   <li>{@link #tickAnnouncement} — pull the next message from the
 *       rotating list and broadcast it. Called on a fixed-rate tick
 *       installed by {@link MythicCorePlugin} via
 *       {@link net.mythicpvp.suite.scheduler.MythicScheduler}.
 * </ul>
 *
 * <p>Cross-shard echo: when a NETWORK broadcast arrives, the
 * originating shard would receive its own message back. We tag every
 * payload with the originating shard id and ignore self-echoes so the
 * sender doesn't see the message twice.
 */
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

    /** Reload runtime config from the supplied YAML. Idempotent. */
    public void load(@NotNull MythicConfig config) {
        this.enabled = config.getBoolean("announcements.enabled", true);
        this.intervalSeconds = Math.max(5, config.getInt("announcements.interval-seconds", 300));
        this.announcementMessages = List.copyOf(config.getStringList("announcements.messages"));
        this.broadcastFormat = config.getString(
                "broadcast.format", "&#FF00F8Broadcast &8» &#FFFFFF%message%");
        // Reset rotation so a config reload starts from the top.
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

    /**
     * Network-wide broadcast — used by {@code /broadcast <message>} and
     * by the announcement tick. {@code rawMessage} is the user/template
     * text; the configured format is applied before the wire send.
     */
    public void broadcast(@NotNull String rawMessage) {
        String formatted = applyFormat(rawMessage);
        publish(formatted);
        // Apply locally too — receivers (including us) consume from the
        // protocol channel, but the originating shard short-circuits to
        // avoid double-render. So fire the local render path explicitly.
        renderLocally(formatted);
    }

    /**
     * Rotate to the next announcement and broadcast it. Returns the
     * line that was sent, or {@code null} if announcements are disabled
     * or no messages are configured.
     */
    public String tickAnnouncement() {
        if (!enabled || announcementMessages.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(nextIndex.getAndIncrement(), announcementMessages.size());
        String raw = announcementMessages.get(index);
        broadcast(raw);
        return raw;
    }

    // ── Internals ────────────────────────────────────────────────────

    private void publish(@NotNull String formatted) {
        protocolManager.publish(CHANNEL, new BroadcastNotice(formatted, shardId));
    }

    private void receive(@NotNull BroadcastNotice notice) {
        if (notice.origin().equals(shardId)) {
            // Originating shard rendered locally already; skip echo.
            return;
        }
        renderLocally(notice.message());
    }

    private void renderLocally(@NotNull String formatted) {
        Component component = MythicHex.colorize(formatted);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        // Also push to console so ops see the broadcast.
        Bukkit.getConsoleSender().sendMessage(component);
    }

    @NotNull
    private String applyFormat(@NotNull String rawMessage) {
        return interpolate(broadcastFormat, Map.of("message", rawMessage));
    }

    /**
     * Tiny `%key%` interpolator — same shape as PlaceholderResolver but
     * without the regex overhead since we only ever expand %message%.
     */
    @NotNull
    private static String interpolate(@NotNull String template, @NotNull Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}

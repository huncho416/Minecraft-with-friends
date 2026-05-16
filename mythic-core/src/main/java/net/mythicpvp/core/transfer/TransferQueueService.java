package net.mythicpvp.core.transfer;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TransferQueueService {

    private static final long DRAIN_PERIOD_TICKS = 20L;
    private static final long ANNOUNCE_PERIOD_TICKS = 100L;
    private static final int DEFAULT_PRIORITY = 1000;

    private final ProxyTransferService transferService;
    private final RankService ranks;
    private final GrantService grants;

    private final Map<UUID, QueueEntry> entries = new ConcurrentHashMap<>();
    private volatile boolean paused;
    private volatile boolean enabled = true;
    private volatile int drainPerTick = 1;
    private volatile java.util.function.Consumer<UUID> onQueueChange = uuid -> {};

    public void setOnQueueChange(@NotNull java.util.function.Consumer<UUID> handler) {
        this.onQueueChange = handler;
    }

    private void fireChange(@NotNull UUID player) {
        try {
            onQueueChange.accept(player);
        } catch (RuntimeException ignored) {
        }
    }

    public TransferQueueService(@NotNull JavaPlugin plugin,
                                @NotNull ProxyTransferService transferService,
                                @NotNull RankService ranks,
                                @NotNull GrantService grants) {
        this.transferService = transferService;
        this.ranks = ranks;
        this.grants = grants;
        MythicScheduler.runTimer(plugin, this::drainTick, DRAIN_PERIOD_TICKS, DRAIN_PERIOD_TICKS);
        MythicScheduler.runTimer(plugin, this::announceTick, ANNOUNCE_PERIOD_TICKS, ANNOUNCE_PERIOD_TICKS);
    }

    public boolean enqueue(@NotNull Player player, @NotNull String targetShard) {
        if (!enabled) {
            return transferService.transfer(player, targetShard);
        }
        UUID id = player.getUniqueId();
        entries.put(id, new QueueEntry(id, targetShard, weightFor(id), System.nanoTime()));
        Integer pos = position(id);
        player.sendMessage(MythicHex.colorize(
                "&#D2D8E0Queued for &#FFFFFF" + targetShard
                        + "&#D2D8E0 — position &#FFFFFF#" + (pos == null ? "?" : pos)
                        + "&#D2D8E0 of &#FFFFFF" + entries.size()));
        fireChange(id);
        return true;
    }

    public void cancel(@NotNull UUID player) {
        if (entries.remove(player) != null) {
            fireChange(player);
        }
    }

    @Nullable
    public Integer position(@NotNull UUID player) {
        if (!entries.containsKey(player)) {
            return null;
        }
        List<QueueEntry> ordered = sortedSnapshot();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).player.equals(player)) {
                return i + 1;
            }
        }
        return null;
    }

    @Nullable
    public QueueStatus statusFor(@NotNull UUID player) {
        if (!entries.containsKey(player)) {
            return null;
        }
        List<QueueEntry> ordered = sortedSnapshot();
        for (int i = 0; i < ordered.size(); i++) {
            QueueEntry e = ordered.get(i);
            if (e.player.equals(player)) {
                return new QueueStatus(i + 1, ordered.size(), e.shard);
            }
        }
        return null;
    }

    public record QueueStatus(int position, int total, @NotNull String shard) {
    }

    public int size() {
        return entries.size();
    }

    public boolean paused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            entries.clear();
        }
    }

    public int drainPerTick() {
        return drainPerTick;
    }

    public void setDrainPerTick(int drainPerTick) {
        this.drainPerTick = Math.max(0, drainPerTick);
    }

    public boolean skipNext() {
        List<QueueEntry> ordered = sortedSnapshot();
        if (ordered.isEmpty()) {
            return false;
        }
        QueueEntry next = ordered.get(0);
        entries.remove(next.player);
        Player p = Bukkit.getPlayer(next.player);
        if (p != null && p.isOnline()) {
            transferService.transfer(p, next.shard);
        }
        fireChange(next.player);
        return true;
    }

    public void clear() {
        entries.clear();
    }

    private void announceTick() {
        if (entries.isEmpty()) {
            return;
        }
        List<QueueEntry> ordered = sortedSnapshot();
        for (int i = 0; i < ordered.size(); i++) {
            QueueEntry entry = ordered.get(i);
            Player p = Bukkit.getPlayer(entry.player);
            if (p == null || !p.isOnline()) {
                continue;
            }
            p.sendMessage(MythicHex.colorize(
                    "&#D2D8E0Queue position &#FFFFFF#" + (i + 1)
                            + "&#D2D8E0 / &#FFFFFF" + ordered.size()
                            + "&#D2D8E0 → &#FFFFFF" + entry.shard
                            + (paused ? " &#FF8A8A(paused)" : "")));
        }
    }

    private void drainTick() {
        if (paused || !enabled || entries.isEmpty()) {
            return;
        }
        List<QueueEntry> ordered = sortedSnapshot();
        int sent = 0;
        for (QueueEntry entry : ordered) {
            if (sent >= drainPerTick) {
                break;
            }
            Player p = Bukkit.getPlayer(entry.player);
            if (p == null || !p.isOnline()) {
                entries.remove(entry.player);
                fireChange(entry.player);
                continue;
            }
            entries.remove(entry.player);
            if (transferService.transfer(p, entry.shard)) {
                sent++;
            }
            fireChange(entry.player);
        }
    }

    @NotNull
    private List<QueueEntry> sortedSnapshot() {
        List<QueueEntry> snapshot = new ArrayList<>(entries.values());
        snapshot.sort(Comparator
                .comparingInt((QueueEntry e) -> e.priority)
                .thenComparingLong(e -> e.enqueuedAtNanos));
        return snapshot;
    }

    private int weightFor(@NotNull UUID uuid) {
        String rankId = grants.activeRank(uuid);
        CoreRank rank = ranks.get(rankId);
        if (rank == null) {
            rank = ranks.get("default");
        }
        return rank == null ? DEFAULT_PRIORITY : rank.weight();
    }

    private static final class QueueEntry {
        final UUID player;
        final String shard;
        final int priority;
        final long enqueuedAtNanos;

        QueueEntry(@NotNull UUID player, @NotNull String shard, int priority, long enqueuedAtNanos) {
            this.player = player;
            this.shard = shard;
            this.priority = priority;
            this.enqueuedAtNanos = enqueuedAtNanos;
        }
    }
}

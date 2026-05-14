package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public final class MainThreadHydrationSink implements HydrationSink {

    private final JavaPlugin plugin;
    private final HydrationSink inner;

    public MainThreadHydrationSink(@NotNull JavaPlugin plugin, @NotNull HydrationSink inner) {
        this.plugin = plugin;
        this.inner = inner;
    }

    @Override public void applyRank(@NotNull CoreRank rank) { schedule(s -> s.applyRank(rank)); }
    @Override public void removeRank(@NotNull String rankId) { schedule(s -> s.removeRank(rankId)); }
    @Override public void applyGrant(@NotNull RankGrant grant) { schedule(s -> s.applyGrant(grant)); }
    @Override public void removeGrant(long grantId) { schedule(s -> s.removeGrant(grantId)); }
    @Override public void applyPunishment(@NotNull PunishmentRecord record) { schedule(s -> s.applyPunishment(record)); }
    @Override public void removePunishment(long punishmentId) { schedule(s -> s.removePunishment(punishmentId)); }
    @Override public void applyTemplate(@NotNull PunishmentTemplate template) { schedule(s -> s.applyTemplate(template)); }
    @Override public void removeTemplate(@NotNull String title) { schedule(s -> s.removeTemplate(title)); }
    @Override public void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active) {
        schedule(s -> s.applyBlacklist(target, targetName, active));
    }

    private void schedule(@NotNull Consumer<HydrationSink> action) {

        if (plugin.getServer().isPrimaryThread()) {
            action.accept(inner);
            return;
        }

        MythicScheduler.runSync(plugin, () -> action.accept(inner));
    }
}

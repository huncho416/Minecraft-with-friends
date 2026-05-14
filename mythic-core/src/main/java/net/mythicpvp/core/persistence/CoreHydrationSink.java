package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.rank.RankService;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Hydration sink backed by mythic-core's three runtime services.
 * Routes inbound rows directly into the {@code apply*} / {@code remove*}
 * methods that bypass the persistence gateway (so we don't echo back to
 * STDB on every row).
 *
 * <p>Threading: this class is **not** thread-safe by itself. Wrap it in
 * {@link MainThreadHydrationSink} when the underlying services need
 * Bukkit-main-thread access.
 *
 * <p>Maintains an in-memory {@code blacklist} map mirroring the
 * {@code punishment_blacklist} table — a future enforcement hook can
 * read it on login. For now the sink only tracks the state; gating is
 * follow-up work flagged in PLAN.
 */
public final class CoreHydrationSink implements HydrationSink {

    private final Logger logger;
    private final RankService rankService;
    private final GrantService grantService;
    private final PunishmentService punishmentService;
    private final ConcurrentMap<UUID, String> blacklist = new ConcurrentHashMap<>();

    public CoreHydrationSink(
            @NotNull Logger logger,
            @NotNull RankService rankService,
            @NotNull GrantService grantService,
            @NotNull PunishmentService punishmentService) {
        this.logger = logger;
        this.rankService = rankService;
        this.grantService = grantService;
        this.punishmentService = punishmentService;
    }

    @Override public void applyRank(@NotNull CoreRank rank) { rankService.applyRank(rank); }
    @Override public void removeRank(@NotNull String rankId) { rankService.removeRank(rankId); }

    @Override public void applyGrant(@NotNull RankGrant grant) { grantService.applyGrant(grant); }
    @Override public void removeGrant(long grantId) { grantService.removeGrant(grantId); }

    @Override public void applyPunishment(@NotNull PunishmentRecord record) { punishmentService.applyRecord(record); }
    @Override public void removePunishment(long punishmentId) { punishmentService.removeRecord(punishmentId); }

    @Override public void applyTemplate(@NotNull PunishmentTemplate template) { punishmentService.applyTemplateRow(template); }
    @Override public void removeTemplate(@NotNull String title) { punishmentService.removeTemplateRow(title); }

    @Override
    public void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active) {
        if (active) {
            blacklist.put(target, targetName);
        } else {
            blacklist.remove(target);
        }
    }

    /** Read-only snapshot of currently-blacklisted player UUIDs. */
    @NotNull
    public java.util.Set<UUID> blacklistedUuids() {
        return java.util.Set.copyOf(blacklist.keySet());
    }

    /** Convenience for login-time enforcement; lazily exposed for future hooks. */
    public boolean isBlacklisted(@NotNull UUID target) {
        return blacklist.containsKey(target);
    }

    @SuppressWarnings("unused") // logger reserved for future apply* error handling
    private void logSwallowed(@NotNull String op, @NotNull Throwable error) {
        logger.warning("[hydration] " + op + " failed: " + error.getMessage());
    }
}

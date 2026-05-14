package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Inbound state sink for STDB hydration.
 *
 * <p>The {@link PersistenceGateway} drives outbound writes; this sink
 * receives the inbound row events that flow back from subscriptions.
 * One method per table — each is called once per row delivered by STDB
 * (initial snapshot + live updates).
 *
 * <p>All callbacks must be **idempotent** because:
 * <ul>
 *   <li>STDB echoes back rows we wrote ourselves; duplicate-applying our
 *       own writes must be a no-op.
 *   <li>Reconnects re-deliver the entire snapshot.
 * </ul>
 *
 * <p>Implementations bridge between the STDB driver thread and the
 * Bukkit/Folia main thread — see {@link MainThreadHydrationSink}.
 */
public interface HydrationSink {

    /** Insert or replace a rank definition by id. */
    void applyRank(@NotNull CoreRank rank);

    /** Remove a rank definition by id. */
    void removeRank(@NotNull String rankId);

    /** Insert or replace a grant by id. */
    void applyGrant(@NotNull RankGrant grant);

    /** Remove a grant by id (hard delete from local state). */
    void removeGrant(long grantId);

    /** Insert or replace a punishment record by id. */
    void applyPunishment(@NotNull PunishmentRecord record);

    /** Remove a punishment record by id. */
    void removePunishment(long punishmentId);

    /** Insert or replace a template by title (case-insensitive). */
    void applyTemplate(@NotNull PunishmentTemplate template);

    /** Remove a template by title. */
    void removeTemplate(@NotNull String title);

    /** Mark a player as blacklisted (or remove if {@code active} is false). */
    void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active);
}

package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Outbound persistence sink for mythic-core mutations.
 *
 * <p>Production wiring uses {@link StdbPersistenceGateway} which forwards
 * each call to a {@link net.mythicpvp.suite.database.schema.MythicSchema}
 * reducer. Tests use {@link NoopPersistenceGateway} or capturing fakes.
 *
 * <p>All methods are fire-and-forget: callers don't await STDB. The
 * gateway logs failures via {@link java.util.logging.Logger} but never
 * throws — services must keep working when STDB is briefly unavailable.
 */
public interface PersistenceGateway {

    // ── Ranks ────────────────────────────────────────────────────────

    /** Insert-or-update a rank definition. */
    void rankDefine(@NotNull CoreRank rank, boolean seeded);

    /** Remove a rank definition by id. Refused server-side if grants exist. */
    void rankRemove(@NotNull String rankId);

    // ── Grants ───────────────────────────────────────────────────────

    /** Persist a newly-issued grant. */
    void grantIssue(@NotNull RankGrant grant);

    /** Soft-deactivate a grant (kept in history). */
    void grantDeactivate(long grantId);

    /** Hard-remove an inactive grant from history. */
    void grantRemoveInactive(long grantId);

    /** Clear every grant for a player. */
    void grantClear(@NotNull UUID target);

    // ── Punishments ──────────────────────────────────────────────────

    /** Persist a newly-issued punishment. */
    void punishIssue(@NotNull PunishmentRecord record);

    /** Pardon a punishment. */
    void punishPardon(long punishmentId, @NotNull UUID staff, @NotNull String reason);

    /** Bulk-clear active punishments for a player. */
    void punishClearHistory(@NotNull UUID target, @NotNull UUID staff);

    // ── Templates ────────────────────────────────────────────────────

    /** Insert-or-update a punishment template. */
    void templateUpsert(@NotNull PunishmentTemplate template, boolean seeded);

    /** Remove a punishment template by title. */
    void templateRemove(@NotNull String title);

    // ── Blacklist ────────────────────────────────────────────────────

    /** Add a player to the blacklist. */
    void blacklistAdd(@NotNull UUID target, @NotNull String targetName,
                      @NotNull UUID staff, @NotNull String staffName,
                      @NotNull String reason);

    /** Revoke a blacklist entry by id. */
    void blacklistRevoke(long entryId, @NotNull UUID staff, @NotNull String reason);

    // ── Appeals ──────────────────────────────────────────────────────

    /** File an appeal against a punishment. */
    void appealOpen(long punishmentId, @NotNull UUID target, @NotNull String message);

    /**
     * Staff review of an open appeal. Decision is {@code APPROVED} or
     * {@code DENIED}; the STDB-side reducer auto-pardons the linked
     * punishment when approved.
     */
    void appealReview(long appealId, @NotNull UUID reviewer, @NotNull String decision, @NotNull String notes);

    // ── Cosmetics ────────────────────────────────────────────────────

    /**
     * Persist a cosmetic grant. {@code cosmeticType} must be one of the
     * STDB wire values (HAT, TITLE, PARTICLE, KILL_EFFECT, WIN_EFFECT,
     * CHAT_TAG). {@code source} typically identifies the system that
     * issued the grant (e.g. {@code "RANK_BUNDLE"}, {@code "STAFF"});
     * {@code reference} carries a system-specific id (e.g. the rank id
     * for bundle grants, or the grant-id string).
     */
    void cosmeticGrant(@NotNull UUID player,
                       @NotNull String cosmeticId,
                       @NotNull String cosmeticType,
                       @NotNull String source,
                       @NotNull String reference);

    // ── Hydration ────────────────────────────────────────────────────

    /**
     * Subscribe to every Phase 3 table and route inbound row events into
     * {@code sink}. Implementations:
     * <ul>
     *   <li>Are idempotent w.r.t. already-applied rows (STDB echoes back
     *       writes we made ourselves).
     *   <li>Auto-reconnect via the underlying STDB connection.
     *   <li>For the no-op gateway, simply do nothing — single-server
     *       deployments need no hydration.
     * </ul>
     *
     * <p>Call once after services are constructed and any local YAML
     * seeding has completed. Returns immediately; rows arrive
     * asynchronously on whatever thread the sink is configured to use.
     */
    void hydrate(@NotNull HydrationSink sink);
}

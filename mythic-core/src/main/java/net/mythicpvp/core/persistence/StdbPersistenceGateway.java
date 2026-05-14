package net.mythicpvp.core.persistence;

import com.google.gson.Gson;
import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.suite.database.ReducerResult;
import net.mythicpvp.suite.database.schema.GrantSource;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.database.schema.PunishmentKind;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Production gateway: forwards every mutation to {@link MythicSchema}.
 *
 * <p>Fire-and-forget. Returned {@link CompletableFuture}s are observed
 * for failure logging only — call sites never block on STDB. The
 * {@link Logger} is the standard JUL one so it integrates with the
 * suite's existing logging configuration without pulling in a new dep.
 *
 * <p>Field mapping notes:
 * <ul>
 *   <li>mythic-core uses {@code expiresAtMillis ≤ 0} for "permanent";
 *       STDB reducers take {@code duration_seconds ≤ 0} for the same.
 *       {@link #durationSecondsFromExpiry} bridges the two.
 *   <li>{@link CoreRank#permissions()} is a {@code List<String>}; STDB
 *       wants a JSON-encoded string column.
 *   <li>{@link CoreRank#dye()} is a Bukkit {@code Material}; STDB stores
 *       the {@code Material.name()} string so the schema doesn't pin a
 *       Bukkit version.
 * </ul>
 */
public final class StdbPersistenceGateway implements PersistenceGateway {

    private static final Gson GSON = new Gson();

    private final Logger logger;
    private final MythicSchema schema;

    public StdbPersistenceGateway(@NotNull Logger logger, @NotNull MythicSchema schema) {
        this.logger = logger;
        this.schema = schema;
    }

    // ── Ranks ────────────────────────────────────────────────────────

    @Override
    public void rankDefine(@NotNull CoreRank rank, boolean seeded) {
        observe("rankDefine " + rank.id(),
                schema.rankDefine(
                        rank.id(),
                        rank.name(),
                        rank.color(),
                        rank.dye().name(),
                        rank.prefix(),
                        rank.suffix(),
                        rank.weight(),
                        rank.staff(),
                        rank.donator(),
                        rank.parent(),
                        GSON.toJson(rank.permissions()),
                        rank.chatPrefix(),
                        rank.chatFormat(),
                        rank.tabPrefix(),
                        rank.tabFormat(),
                        rank.nametagPrefix(),
                        rank.nametagFormat(),
                        seeded));
    }

    @Override
    public void rankRemove(@NotNull String rankId) {
        observe("rankRemove " + rankId, schema.rankRemove(rankId));
    }

    // ── Grants ───────────────────────────────────────────────────────

    @Override
    public void grantIssue(@NotNull RankGrant grant) {
        observe("grantIssue " + grant.targetUuid() + " -> " + grant.rankId(),
                schema.grantIssue(
                        grant.targetUuid(),
                        grant.targetName(),
                        grant.rankId(),
                        grant.executorUuid(),
                        grant.executorName(),
                        grant.reason(),
                        // mythic-core doesn't track grant source today;
                        // STAFF is the safe default — grants come from the
                        // /grant menu which is staff-gated. Future work:
                        // thread an explicit source through GrantService.
                        GrantSource.STAFF,
                        durationSecondsFromExpiry(grant.createdAtMillis(), grant.expiresAtMillis())));
    }

    @Override
    public void grantDeactivate(long grantId) {
        observe("grantDeactivate " + grantId, schema.grantDeactivate(grantId));
    }

    @Override
    public void grantRemoveInactive(long grantId) {
        observe("grantRemoveInactive " + grantId, schema.grantRemoveInactive(grantId));
    }

    @Override
    public void grantClear(@NotNull UUID target) {
        observe("grantClear " + target, schema.grantClear(target));
    }

    // ── Punishments ──────────────────────────────────────────────────

    @Override
    public void punishIssue(@NotNull PunishmentRecord record) {
        observe("punishIssue " + record.targetUuid() + " " + record.type(),
                schema.punishIssue(
                        record.targetUuid(),
                        record.targetName(),
                        record.staffUuid(),
                        record.staffName(),
                        kindFor(record.type()),
                        record.reason(),
                        record.proof(),
                        durationSecondsFromExpiry(record.createdAtMillis(), record.expiresAtMillis()),
                        record.silent(),
                        record.clearInventory(),
                        record.server()));
    }

    @Override
    public void punishPardon(long punishmentId, @NotNull UUID staff, @NotNull String reason) {
        observe("punishPardon " + punishmentId, schema.punishPardon(punishmentId, staff, reason));
    }

    @Override
    public void punishClearHistory(@NotNull UUID target, @NotNull UUID staff) {
        observe("punishClearHistory " + target, schema.punishClearHistory(target, staff));
    }

    // ── Templates ────────────────────────────────────────────────────

    @Override
    public void templateUpsert(@NotNull PunishmentTemplate template, boolean seeded) {
        observe("templateUpsert " + template.title(),
                schema.templateUpsert(
                        template.title(),
                        categoryFor(template.category()),
                        template.duration(),
                        template.information(),
                        seeded));
    }

    @Override
    public void templateRemove(@NotNull String title) {
        observe("templateRemove " + title, schema.templateRemove(title));
    }

    // ── Blacklist ────────────────────────────────────────────────────

    @Override
    public void blacklistAdd(@NotNull UUID target, @NotNull String targetName,
                             @NotNull UUID staff, @NotNull String staffName,
                             @NotNull String reason) {
        observe("blacklistAdd " + target,
                schema.blacklistAdd(target, targetName, staff, staffName, reason));
    }

    @Override
    public void blacklistRevoke(long entryId, @NotNull UUID staff, @NotNull String reason) {
        observe("blacklistRevoke " + entryId, schema.blacklistRevoke(entryId, staff, reason));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /** Map mythic-core's {@link PunishmentType} to the schema's wire enum. */
    @NotNull
    private static PunishmentKind kindFor(@NotNull PunishmentType type) {
        return switch (type) {
            case WARN -> PunishmentKind.WARN;
            case MUTE -> PunishmentKind.MUTE;
            case TEMP_MUTE -> PunishmentKind.TEMP_MUTE;
            case BAN -> PunishmentKind.BAN;
            case TEMP_BAN -> PunishmentKind.TEMP_BAN;
            case BLACKLIST -> PunishmentKind.BLACKLIST;
        };
    }

    @NotNull
    private static net.mythicpvp.suite.database.schema.PunishmentCategory categoryFor(
            @NotNull PunishmentCategory category) {
        return switch (category) {
            case WARN -> net.mythicpvp.suite.database.schema.PunishmentCategory.WARN;
            case MUTE -> net.mythicpvp.suite.database.schema.PunishmentCategory.MUTE;
            case BAN -> net.mythicpvp.suite.database.schema.PunishmentCategory.BAN;
            case BLACKLIST -> net.mythicpvp.suite.database.schema.PunishmentCategory.BLACKLIST;
        };
    }

    /**
     * Convert mythic-core's {@code expiresAtMillis} (absolute epoch ms,
     * 0 = permanent) into the duration-seconds value the STDB reducers
     * expect (0 = permanent, otherwise seconds-from-now).
     *
     * <p>STDB computes {@code expires_at = now + duration_seconds}
     * server-side. We send the relative duration so any clock skew
     * between the proxy and STDB stays bounded.
     */
    static long durationSecondsFromExpiry(long createdAtMillis, long expiresAtMillis) {
        if (expiresAtMillis <= 0) {
            return 0;
        }
        long deltaMillis = expiresAtMillis - createdAtMillis;
        if (deltaMillis <= 0) {
            // Expiry not after creation — defensive: defer to STDB's
            // permanent semantics (0) rather than silently clamp to a
            // 1-second punishment that the user didn't intend.
            return 0;
        }
        // Sub-second deltas would truncate to 0 (= permanent) which is
        // wrong for an actually-temporary punishment; clamp to 1s.
        return Math.max(1, deltaMillis / 1000);
    }

    private void observe(@NotNull String op, @NotNull CompletableFuture<ReducerResult> future) {
        future.whenComplete((result, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, () -> "[stdb] " + op + " failed: " + error.getMessage());
            } else if (result != null && !result.success()) {
                logger.log(Level.WARNING, () -> "[stdb] " + op + " rejected: " + result.error());
            }
        });
    }
}

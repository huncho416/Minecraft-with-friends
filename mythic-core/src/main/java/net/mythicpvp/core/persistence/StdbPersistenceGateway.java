package net.mythicpvp.core.persistence;

import com.google.gson.Gson;
import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.suite.database.ReducerResult;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.TableEvent;
import net.mythicpvp.suite.database.schema.GrantSource;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.database.schema.PunishmentKind;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.BlacklistEntryRow;
import net.mythicpvp.suite.database.schema.dto.PunishmentRow;
import net.mythicpvp.suite.database.schema.dto.PunishmentTemplateRow;
import net.mythicpvp.suite.database.schema.dto.RankDefinitionRow;
import net.mythicpvp.suite.database.schema.dto.RankGrantRow;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
    private final SpacetimeConnection connection;

    /**
     * Construct with the schema client + the underlying connection. The
     * connection is needed for {@link #hydrate} which subscribes to
     * tables. Both must reference the same STDB host so reducer calls
     * and subscription events are consistent.
     */
    public StdbPersistenceGateway(
            @NotNull Logger logger,
            @NotNull MythicSchema schema,
            @NotNull SpacetimeConnection connection) {
        this.logger = logger;
        this.schema = schema;
        this.connection = connection;
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

    // ── Appeals ──────────────────────────────────────────────────────

    @Override
    public void appealOpen(long punishmentId, @NotNull UUID target, @NotNull String message) {
        observe("appealOpen " + punishmentId, schema.appealOpen(punishmentId, target, message));
    }

    @Override
    public void appealReview(long appealId, @NotNull UUID reviewer, @NotNull String decision, @NotNull String notes) {
        // Map the gameplay-side string to the schema enum. Anything other
        // than APPROVED is treated as DENIED so a typo doesn't accidentally
        // approve.
        net.mythicpvp.suite.database.schema.MythicSchema.AppealDecision dec =
                "APPROVED".equalsIgnoreCase(decision)
                        ? net.mythicpvp.suite.database.schema.MythicSchema.AppealDecision.APPROVED
                        : net.mythicpvp.suite.database.schema.MythicSchema.AppealDecision.DENIED;
        observe("appealReview " + appealId, schema.appealReview(appealId, reviewer, dec, notes));
    }

    // ── Cosmetics ────────────────────────────────────────────────────

    @Override
    public void cosmeticGrant(@NotNull UUID player, @NotNull String cosmeticId,
                              @NotNull String cosmeticType, @NotNull String source,
                              @NotNull String reference) {
        net.mythicpvp.suite.database.schema.StdbCosmeticType type =
                net.mythicpvp.suite.database.schema.StdbCosmeticType.fromWire(cosmeticType);
        if (type == null) {
            // Unknown type — log + drop rather than throwing. Caller is
            // fire-and-forget; we don't want one bad lookup to break the
            // surrounding rank grant.
            logger.warning("[stdb] cosmeticGrant skipped: unknown type " + cosmeticType
                    + " for cosmetic " + cosmeticId);
            return;
        }
        observe("cosmeticGrant " + cosmeticId + " -> " + player,
                schema.cosmeticGrant(player, cosmeticId, type, source, reference));
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

    // ── Hydration ────────────────────────────────────────────────────

    @Override
    public void hydrate(@NotNull HydrationSink sink) {
        // Subscribe to every Phase 3 table. The connection auto-resubscribes
        // on reconnect, so a transient outage doesn't lose us state — STDB
        // re-delivers the snapshot, and our apply* sinks are idempotent
        // by id / title.
        subscribe(TableNames.RANK_DEFINITIONS, RankDefinitionRow.class,
                row -> sink.applyRank(toCoreRank(row)),
                row -> sink.removeRank(row.id()));
        subscribe(TableNames.RANK_GRANTS, RankGrantRow.class,
                row -> sink.applyGrant(toRankGrant(row)),
                row -> sink.removeGrant(row.id()));
        subscribe(TableNames.PUNISHMENTS, PunishmentRow.class,
                row -> sink.applyPunishment(toPunishmentRecord(row)),
                row -> sink.removePunishment(row.id()));
        subscribe(TableNames.PUNISHMENT_TEMPLATES, PunishmentTemplateRow.class,
                row -> sink.applyTemplate(toPunishmentTemplate(row)),
                row -> sink.removeTemplate(row.title()));
        subscribe(TableNames.PUNISHMENT_BLACKLIST, BlacklistEntryRow.class,
                row -> applyBlacklistRow(sink, row),
                row -> applyBlacklistRow(sink, row));
        logger.info("[stdb] hydration subscriptions registered for 5 tables");
    }

    /**
     * Subscribe to a single STDB table, deserialize each event payload
     * into {@code dtoType}, and dispatch to either the apply or remove
     * callback based on the operation kind.
     */
    private <D> void subscribe(
            @NotNull String table,
            @NotNull Class<D> dtoType,
            @NotNull Consumer<D> onUpsert,
            @NotNull Consumer<D> onDelete) {
        connection.subscribeTable(table, event -> handleEvent(table, dtoType, event, onUpsert, onDelete));
    }

    private <D> void handleEvent(
            @NotNull String table,
            @NotNull Class<D> dtoType,
            @NotNull TableEvent event,
            @NotNull Consumer<D> onUpsert,
            @NotNull Consumer<D> onDelete) {
        D row;
        try {
            row = GSON.fromJson(event.payload(), dtoType);
        } catch (Exception parseError) {
            logger.warning("[hydration] " + table + " bad row "
                    + event.payload() + ": " + parseError.getMessage());
            return;
        }
        if (row == null) {
            return;
        }
        try {
            if ("delete".equalsIgnoreCase(event.operation())) {
                onDelete.accept(row);
            } else {
                onUpsert.accept(row);
            }
        } catch (Exception applyError) {
            logger.warning("[hydration] " + table + " apply failed: " + applyError.getMessage());
        }
    }

    private static void applyBlacklistRow(@NotNull HydrationSink sink, @NotNull BlacklistEntryRow row) {
        UUID target;
        try {
            target = UUID.fromString(row.target_uuid());
        } catch (IllegalArgumentException e) {
            return;
        }
        sink.applyBlacklist(target, row.target_name(), row.active());
    }

    // ── DTO → domain conversions ────────────────────────────────────

    @NotNull
    static CoreRank toCoreRank(@NotNull RankDefinitionRow row) {
        Material dye = matchMaterial(row.dye());
        List<String> permissions = parsePermissions(row.permissions_json());
        return new CoreRank(
                row.id(),
                row.display_name(),
                row.color(),
                dye,
                row.prefix(),
                row.suffix(),
                row.weight(),
                row.staff(),
                row.donator(),
                row.parent(),
                permissions,
                row.chat_prefix(),
                row.chat_format(),
                row.tab_prefix(),
                row.tab_format(),
                row.nametag_prefix(),
                row.nametag_format());
    }

    @NotNull
    static RankGrant toRankGrant(@NotNull RankGrantRow row) {
        return new RankGrant(
                row.id(),
                UUID.fromString(row.target_uuid()),
                row.target_name(),
                row.rank_id(),
                UUID.fromString(row.executor_uuid()),
                row.executor_name(),
                row.reason(),
                microsToMillis(row.created_at()),
                microsToMillis(row.expires_at_micros()),
                row.active());
    }

    @NotNull
    static PunishmentRecord toPunishmentRecord(@NotNull PunishmentRow row) {
        PunishmentType type = punishmentTypeFor(row.kind());
        // Active=false on STDB means pardoned/expired. mythic-core's
        // "pardoned" is the closest semantic match; the audit trail of
        // who/why pardoned lives in pardoned_by/pardoned_at_micros which
        // mythic-core's record doesn't model.
        boolean pardoned = !row.active();
        return new PunishmentRecord(
                row.id(),
                UUID.fromString(row.target_uuid()),
                row.target_name(),
                UUID.fromString(row.staff_uuid()),
                row.staff_name(),
                type,
                row.reason(),
                row.proof(),
                microsToMillis(row.issued_at()),
                microsToMillis(row.expires_at_micros()),
                row.silent(),
                row.clear_inventory(),
                pardoned,
                row.server());
    }

    @NotNull
    static PunishmentTemplate toPunishmentTemplate(@NotNull PunishmentTemplateRow row) {
        return new PunishmentTemplate(
                punishmentCategoryFor(row.category()),
                row.duration(),
                row.title(),
                row.information());
    }

    // ── Mapping helpers ─────────────────────────────────────────────

    @NotNull
    private static Material matchMaterial(@NotNull String name) {
        Material m = Material.matchMaterial(name);
        return m == null ? Material.LIGHT_GRAY_DYE : m;
    }

    @NotNull
    private static List<String> parsePermissions(@NotNull String json) {
        if (json.isEmpty()) {
            return List.of();
        }
        try {
            String[] arr = GSON.fromJson(json, String[].class);
            return arr == null ? List.of() : List.of(arr);
        } catch (Exception ignore) {
            return List.of();
        }
    }

    /**
     * STDB Timestamps are microseconds since Unix epoch; mythic-core
     * uses millis. {@code 0} stays {@code 0} (the "permanent" sentinel).
     */
    static long microsToMillis(long micros) {
        return micros == 0L ? 0L : micros / 1_000L;
    }

    @NotNull
    private static PunishmentType punishmentTypeFor(@NotNull String wire) {
        return switch (wire) {
            case "WARN" -> PunishmentType.WARN;
            case "MUTE" -> PunishmentType.MUTE;
            case "TEMP_MUTE" -> PunishmentType.TEMP_MUTE;
            case "BAN" -> PunishmentType.BAN;
            case "TEMP_BAN" -> PunishmentType.TEMP_BAN;
            case "BLACKLIST" -> PunishmentType.BLACKLIST;
            // KICK exists on STDB for proxy emissions; mythic-core has no
            // kick punishment type — fall through to BAN as the closest
            // login-blocking analogue. In practice the kick rows never
            // arrive at the game server from STDB hydration since proxy
            // kicks are ephemeral.
            default -> PunishmentType.BAN;
        };
    }

    @NotNull
    private static PunishmentCategory punishmentCategoryFor(@NotNull String wire) {
        return switch (wire) {
            case "WARN" -> PunishmentCategory.WARN;
            case "MUTE" -> PunishmentCategory.MUTE;
            case "BAN" -> PunishmentCategory.BAN;
            case "BLACKLIST" -> PunishmentCategory.BLACKLIST;
            default -> PunishmentCategory.WARN;
        };
    }
}

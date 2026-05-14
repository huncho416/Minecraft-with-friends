package net.mythicpvp.suite.database.schema;

import net.mythicpvp.suite.database.ReducerResult;
import net.mythicpvp.suite.database.SpacetimeConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class MythicSchema {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final SpacetimeConnection connection;

    public MythicSchema(@NotNull SpacetimeConnection connection) {
        this.connection = connection;
    }

    @NotNull
    public CompletableFuture<ReducerResult> playerSetRank(@NotNull UUID uuid, @NotNull String rank) {
        return call(ReducerNames.PLAYER_SET_RANK, hyphenated(uuid), rank);
    }

    @NotNull
    public CompletableFuture<ReducerResult> playerSetRegion(@NotNull UUID uuid, @NotNull String region) {
        return call(ReducerNames.PLAYER_SET_REGION, hyphenated(uuid), region);
    }

    @NotNull
    public CompletableFuture<ReducerResult> registryAnnounce(
            @NotNull String shardId,
            @NotNull ServerRole role,
            @NotNull String region,
            @NotNull String address,
            int maxPlayers,
            int schemaVersion) {
        return call(
                ReducerNames.REGISTRY_ANNOUNCE,
                shardId, role.wireValue(), region, address, maxPlayers, schemaVersion);
    }

    @NotNull
    public CompletableFuture<ReducerResult> registryHeartbeat(
            @NotNull String shardId,
            @NotNull ServerStatus status,
            int playerCount,
            float tps,
            float heapLoad) {
        return call(
                ReducerNames.REGISTRY_HEARTBEAT,
                shardId, status.wireValue(), playerCount, tps, heapLoad);
    }

    @NotNull
    public CompletableFuture<ReducerResult> registryDrain(@NotNull String shardId) {
        return call(ReducerNames.REGISTRY_DRAIN, shardId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> sessionLogin(
            @NotNull UUID uuid,
            @NotNull String username,
            @NotNull String shardId,
            long proxySessionId,
            @NotNull String ipHash,
            @NotNull String region) {
        return call(
                ReducerNames.SESSION_LOGIN,
                hyphenated(uuid), username, shardId, proxySessionId, ipHash, region);
    }

    @NotNull
    public CompletableFuture<ReducerResult> sessionLogout(@NotNull UUID uuid, @NotNull String reason) {
        return call(ReducerNames.SESSION_LOGOUT, hyphenated(uuid), reason);
    }

    @NotNull
    public CompletableFuture<ReducerResult> sessionRoute(
            @NotNull UUID uuid, @NotNull String newShardId, @NotNull String reason) {
        return call(ReducerNames.SESSION_ROUTE, hyphenated(uuid), newShardId, reason);
    }

    @NotNull
    public CompletableFuture<ReducerResult> sessionTouch(@NotNull UUID uuid) {
        return call(ReducerNames.SESSION_TOUCH, hyphenated(uuid));
    }

    @NotNull
    public CompletableFuture<ReducerResult> sessionReap(long olderThanSeconds) {
        if (olderThanSeconds < 0) {
            throw new IllegalArgumentException("olderThanSeconds must be >= 0");
        }
        return call(ReducerNames.SESSION_REAP, olderThanSeconds);
    }

    @NotNull
    public CompletableFuture<ReducerResult> punishIssue(
            @NotNull UUID target,
            @NotNull String targetName,
            @NotNull UUID staff,
            @NotNull String staffName,
            @NotNull PunishmentKind kind,
            @NotNull String reason,
            @NotNull String proof,
            long durationSeconds,
            boolean silent,
            boolean clearInventory,
            @NotNull String server) {
        return call(
                ReducerNames.PUNISH_ISSUE,
                hyphenated(target), targetName,
                hyphenated(staff), staffName,
                kind.wireValue(),
                reason, proof, durationSeconds,
                silent, clearInventory, server);
    }

    @NotNull
    public CompletableFuture<ReducerResult> punishPardon(
            long punishmentId, @NotNull UUID staff, @NotNull String reason) {
        return call(ReducerNames.PUNISH_PARDON, punishmentId, hyphenated(staff), reason);
    }

    @NotNull
    public CompletableFuture<ReducerResult> punishExpire() {
        return call(ReducerNames.PUNISH_EXPIRE);
    }

    @NotNull
    public CompletableFuture<ReducerResult> punishClearHistory(@NotNull UUID target, @NotNull UUID staff) {
        return call(ReducerNames.PUNISH_CLEAR_HISTORY, hyphenated(target), hyphenated(staff));
    }

    @NotNull
    public CompletableFuture<ReducerResult> templateUpsert(
            @NotNull String title,
            @NotNull PunishmentCategory category,
            @NotNull String duration,
            @NotNull String information,
            boolean seeded) {
        if (title.isEmpty()) {
            throw new IllegalArgumentException("template title required");
        }
        return call(
                ReducerNames.TEMPLATE_UPSERT,
                title, category.wireValue(), duration, information, seeded);
    }

    @NotNull
    public CompletableFuture<ReducerResult> templateRemove(@NotNull String title) {
        return call(ReducerNames.TEMPLATE_REMOVE, title);
    }

    @NotNull
    public CompletableFuture<ReducerResult> blacklistAdd(
            @NotNull UUID target,
            @NotNull String targetName,
            @NotNull UUID staff,
            @NotNull String staffName,
            @NotNull String reason) {
        return call(
                ReducerNames.BLACKLIST_ADD,
                hyphenated(target), targetName,
                hyphenated(staff), staffName, reason);
    }

    @NotNull
    public CompletableFuture<ReducerResult> blacklistRevoke(
            long entryId, @NotNull UUID staff, @NotNull String reason) {
        return call(ReducerNames.BLACKLIST_REVOKE, entryId, hyphenated(staff), reason);
    }

    @NotNull
    public CompletableFuture<ReducerResult> rankDefine(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull String color,
            @NotNull String dye,
            @NotNull String prefix,
            @NotNull String suffix,
            int weight,
            boolean staff,
            boolean donator,
            @NotNull String parent,
            @NotNull String permissionsJson,
            @NotNull String chatPrefix,
            @NotNull String chatFormat,
            @NotNull String tabPrefix,
            @NotNull String tabFormat,
            @NotNull String nametagPrefix,
            @NotNull String nametagFormat,
            boolean seeded) {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("rank id required");
        }
        return call(
                ReducerNames.RANK_DEFINE,
                id, displayName, color, dye, prefix, suffix, weight,
                staff, donator, parent, permissionsJson,
                chatPrefix, chatFormat, tabPrefix, tabFormat,
                nametagPrefix, nametagFormat, seeded);
    }

    @NotNull
    public CompletableFuture<ReducerResult> rankRemove(@NotNull String id) {
        return call(ReducerNames.RANK_REMOVE, id);
    }

    @NotNull
    public CompletableFuture<ReducerResult> grantIssue(
            @NotNull UUID target,
            @NotNull String targetName,
            @NotNull String rankId,
            @NotNull UUID executor,
            @NotNull String executorName,
            @NotNull String reason,
            @NotNull GrantSource source,
            long durationSeconds) {
        return call(
                ReducerNames.GRANT_ISSUE,
                hyphenated(target), targetName, rankId,
                hyphenated(executor), executorName, reason,
                source.wireValue(), durationSeconds);
    }

    @NotNull
    public CompletableFuture<ReducerResult> grantDeactivate(long grantId) {
        return call(ReducerNames.GRANT_DEACTIVATE, grantId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> grantRemoveInactive(long grantId) {
        return call(ReducerNames.GRANT_REMOVE_INACTIVE, grantId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> grantClear(@NotNull UUID target) {
        return call(ReducerNames.GRANT_CLEAR, hyphenated(target));
    }

    @NotNull
    public CompletableFuture<ReducerResult> grantExpire() {
        return call(ReducerNames.GRANT_EXPIRE);
    }

    @NotNull
    public CompletableFuture<ReducerResult> appealOpen(
            long punishmentId, @NotNull UUID target, @NotNull String message) {
        return call(ReducerNames.APPEAL_OPEN, punishmentId, hyphenated(target), message);
    }

    @NotNull
    public CompletableFuture<ReducerResult> appealReview(
            long appealId, @NotNull UUID reviewer, @NotNull AppealDecision decision, @NotNull String notes) {
        return call(
                ReducerNames.APPEAL_REVIEW,
                appealId, hyphenated(reviewer), decision.wireValue(), notes);
    }

    public enum AppealDecision {
        APPROVED("APPROVED"),
        DENIED("DENIED");

        private final String wire;

        AppealDecision(@NotNull String wire) {
            this.wire = wire;
        }

        @NotNull
        public String wireValue() {
            return wire;
        }
    }

    @NotNull
    public CompletableFuture<ReducerResult> economyAdjust(
            @NotNull UUID uuid,
            @NotNull StdbCurrency currency,
            long amount,
            @NotNull String source,
            @NotNull String reference) {
        if (amount == 0) {
            throw new IllegalArgumentException("amount must be non-zero");
        }
        return call(
                ReducerNames.ECONOMY_ADJUST,
                hyphenated(uuid), currency.wireValue(), amount, source, reference);
    }

    @NotNull
    public CompletableFuture<ReducerResult> economyTransfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull StdbCurrency currency,
            long amount,
            @NotNull String reference) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("from and to must differ");
        }
        return call(
                ReducerNames.ECONOMY_TRANSFER,
                hyphenated(from), hyphenated(to), currency.wireValue(), amount, reference);
    }

    @NotNull
    public CompletableFuture<ReducerResult> economyRollback(
            @NotNull UUID uuid, long sinceMicros, long untilMicros, @NotNull String reason) {
        if (sinceMicros >= untilMicros) {
            throw new IllegalArgumentException("sinceMicros must be < untilMicros");
        }
        return call(
                ReducerNames.ECONOMY_ROLLBACK,
                hyphenated(uuid), sinceMicros, untilMicros, reason);
    }

    @NotNull
    public CompletableFuture<ReducerResult> cosmeticGrant(
            @NotNull UUID player,
            @NotNull String cosmeticId,
            @NotNull StdbCosmeticType type,
            @NotNull String source,
            @NotNull String reference) {
        return call(
                ReducerNames.COSMETIC_GRANT,
                hyphenated(player), cosmeticId, type.wireValue(), source, reference);
    }

    @NotNull
    public CompletableFuture<ReducerResult> cosmeticEquip(
            @NotNull UUID player, @NotNull StdbCosmeticType type, @NotNull String cosmeticId) {
        return call(
                ReducerNames.COSMETIC_EQUIP,
                hyphenated(player), type.wireValue(), cosmeticId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> friendRequest(@NotNull UUID from, @NotNull UUID to) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("cannot friend self");
        }
        return call(ReducerNames.FRIEND_REQUEST, hyphenated(from), hyphenated(to));
    }

    @NotNull
    public CompletableFuture<ReducerResult> friendAccept(long requestId) {
        return call(ReducerNames.FRIEND_ACCEPT, requestId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> friendRemove(@NotNull UUID owner, @NotNull UUID friend) {
        return call(ReducerNames.FRIEND_REMOVE, hyphenated(owner), hyphenated(friend));
    }

    @NotNull
    public CompletableFuture<ReducerResult> partyCreate(@NotNull UUID leader) {
        return call(ReducerNames.PARTY_CREATE, hyphenated(leader));
    }

    @NotNull
    public CompletableFuture<ReducerResult> partyJoin(long partyId, @NotNull UUID player) {
        return call(ReducerNames.PARTY_JOIN, partyId, hyphenated(player));
    }

    @NotNull
    public CompletableFuture<ReducerResult> partyLeave(long partyId, @NotNull UUID player) {
        return call(ReducerNames.PARTY_LEAVE, partyId, hyphenated(player));
    }

    @NotNull
    public CompletableFuture<ReducerResult> partyDisband(long partyId) {
        return call(ReducerNames.PARTY_DISBAND, partyId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> mailSend(
            @NotNull UUID sender,
            @NotNull UUID recipient,
            @NotNull String subject,
            @NotNull String body,
            @NotNull String attachmentsJson) {
        return call(
                ReducerNames.MAIL_SEND,
                hyphenated(sender), hyphenated(recipient), subject, body, attachmentsJson);
    }

    @NotNull
    public CompletableFuture<ReducerResult> mailMarkRead(long mailId) {
        return call(ReducerNames.MAIL_MARK_READ, mailId);
    }

    @NotNull
    public CompletableFuture<ReducerResult> islandCreate(
            @NotNull String islandId,
            @NotNull UUID owner,
            @NotNull String shardId,
            @NotNull String sizeTier) {
        return call(
                ReducerNames.ISLAND_CREATE,
                islandId, hyphenated(owner), shardId, sizeTier);
    }

    @NotNull
    public CompletableFuture<ReducerResult> skillGrantXp(
            @NotNull UUID player, @NotNull String skill, long xpDelta) {
        if (xpDelta < 0) {
            throw new IllegalArgumentException("xpDelta must be non-negative (Rust signature is u64)");
        }
        return call(ReducerNames.SKILL_GRANT_XP, hyphenated(player), skill, xpDelta);
    }

    @NotNull
    public CompletableFuture<ReducerResult> statIncrement(
            @NotNull UUID player, @NotNull String stat, long delta) {
        return call(ReducerNames.STAT_INCREMENT, hyphenated(player), stat, delta);
    }

    @NotNull
    public CompletableFuture<ReducerResult> statsResetDaily() {
        return call(ReducerNames.STATS_RESET_DAILY);
    }

    @NotNull
    public CompletableFuture<ReducerResult> statsResetWeekly() {
        return call(ReducerNames.STATS_RESET_WEEKLY);
    }

    @NotNull
    public CompletableFuture<ReducerResult> leaderboardRebuild(
            @NotNull String board, @NotNull String timeframe, @NotNull String statKey, int topN) {
        if (topN <= 0) {
            throw new IllegalArgumentException("topN must be positive");
        }
        return call(ReducerNames.LEADERBOARD_REBUILD, board, timeframe, statKey, topN);
    }

    @NotNull
    private CompletableFuture<ReducerResult> call(@NotNull String reducer, @NotNull Object... args) {
        return connection.callReducer(reducer, args);
    }

    @NotNull
    static String hyphenated(@NotNull UUID uuid) {
        String s = uuid.toString();
        if (!UUID_PATTERN.matcher(s).matches()) {
            throw new IllegalArgumentException("UUID.toString() produced unexpected form: " + s);
        }
        return s;
    }
}

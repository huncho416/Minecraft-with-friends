package net.mythicpvp.core.persistence;

import com.google.gson.Gson;
import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.core.social.FriendLink;
import net.mythicpvp.core.social.FriendRequest;
import net.mythicpvp.core.social.LoginStreak;
import net.mythicpvp.core.social.MailMessage;
import net.mythicpvp.core.social.Party;
import net.mythicpvp.core.social.PartyMember;
import net.mythicpvp.suite.database.ReducerResult;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.TableEvent;
import net.mythicpvp.suite.database.schema.GrantSource;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.database.schema.PunishmentKind;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.BlacklistEntryRow;
import net.mythicpvp.suite.database.schema.dto.FriendRequestRow;
import net.mythicpvp.suite.database.schema.dto.FriendRow;
import net.mythicpvp.suite.database.schema.dto.LoginStreakRow;
import net.mythicpvp.suite.database.schema.dto.MailRow;
import net.mythicpvp.suite.database.schema.dto.PartyMemberRow;
import net.mythicpvp.suite.database.schema.dto.PartyRow;
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

public final class StdbPersistenceGateway implements PersistenceGateway {

    private static final Gson GSON = new Gson();

    private final Logger logger;
    private final MythicSchema schema;
    private final SpacetimeConnection connection;

    public StdbPersistenceGateway(
            @NotNull Logger logger,
            @NotNull MythicSchema schema,
            @NotNull SpacetimeConnection connection) {
        this.logger = logger;
        this.schema = schema;
        this.connection = connection;
    }

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

    @Override
    public void appealOpen(long punishmentId, @NotNull UUID target, @NotNull String message) {
        observe("appealOpen " + punishmentId, schema.appealOpen(punishmentId, target, message));
    }

    @Override
    public void appealReview(long appealId, @NotNull UUID reviewer, @NotNull String decision, @NotNull String notes) {

        net.mythicpvp.suite.database.schema.MythicSchema.AppealDecision dec =
                "APPROVED".equalsIgnoreCase(decision)
                        ? net.mythicpvp.suite.database.schema.MythicSchema.AppealDecision.APPROVED
                        : net.mythicpvp.suite.database.schema.MythicSchema.AppealDecision.DENIED;
        observe("appealReview " + appealId, schema.appealReview(appealId, reviewer, dec, notes));
    }

    @Override
    public void cosmeticGrant(@NotNull UUID player, @NotNull String cosmeticId,
                              @NotNull String cosmeticType, @NotNull String source,
                              @NotNull String reference) {
        net.mythicpvp.suite.database.schema.StdbCosmeticType type =
                net.mythicpvp.suite.database.schema.StdbCosmeticType.fromWire(cosmeticType);
        if (type == null) {

            logger.warning("[stdb] cosmeticGrant skipped: unknown type " + cosmeticType
                    + " for cosmetic " + cosmeticId);
            return;
        }
        observe("cosmeticGrant " + cosmeticId + " -> " + player,
                schema.cosmeticGrant(player, cosmeticId, type, source, reference));
    }

    @Override
    public void cosmeticEquip(@NotNull UUID player, @NotNull String cosmeticType, @NotNull String cosmeticId) {
        net.mythicpvp.suite.database.schema.StdbCosmeticType type =
                net.mythicpvp.suite.database.schema.StdbCosmeticType.fromWire(cosmeticType);
        if (type == null) {
            logger.warning("[stdb] cosmeticEquip skipped: unknown type " + cosmeticType
                    + " for cosmetic " + cosmeticId);
            return;
        }
        observe("cosmeticEquip " + cosmeticId + " -> " + player,
                schema.cosmeticEquip(player, type, cosmeticId));
    }

    @Override
    public void friendRequest(@NotNull UUID from, @NotNull UUID to) {
        observe("friendRequest " + from + " -> " + to, schema.friendRequest(from, to));
    }

    @Override
    public void friendAccept(long requestId) {
        observe("friendAccept " + requestId, schema.friendAccept(requestId));
    }

    @Override
    public void friendDeny(long requestId) {
        observe("friendDeny " + requestId, schema.friendDeny(requestId));
    }

    @Override
    public void friendRemove(@NotNull UUID owner, @NotNull UUID friend) {
        observe("friendRemove " + owner + " <-> " + friend, schema.friendRemove(owner, friend));
    }

    @Override
    public void partyCreate(@NotNull UUID leader) {
        observe("partyCreate " + leader, schema.partyCreate(leader));
    }

    @Override
    public void partyJoin(long partyId, @NotNull UUID player) {
        observe("partyJoin " + partyId + " " + player, schema.partyJoin(partyId, player));
    }

    @Override
    public void partyLeave(long partyId, @NotNull UUID player) {
        observe("partyLeave " + partyId + " " + player, schema.partyLeave(partyId, player));
    }

    @Override
    public void partyDisband(long partyId) {
        observe("partyDisband " + partyId, schema.partyDisband(partyId));
    }

    @Override
    public void mailSend(@NotNull UUID sender, @NotNull UUID recipient,
                         @NotNull String subject, @NotNull String body,
                         @NotNull String attachmentsJson) {
        observe("mailSend " + sender + " -> " + recipient,
                schema.mailSend(sender, recipient, subject, body, attachmentsJson));
    }

    @Override
    public void mailMarkRead(long mailId) {
        observe("mailMarkRead " + mailId, schema.mailMarkRead(mailId));
    }

    @Override
    public void loginStreakRecord(@NotNull UUID player, long loginMillis, int streak) {
        observe("loginStreakRecord " + player + " streak=" + streak,
                schema.loginStreakRecord(player, loginMillis, streak));
    }

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

    static long durationSecondsFromExpiry(long createdAtMillis, long expiresAtMillis) {
        if (expiresAtMillis <= 0) {
            return 0;
        }
        long deltaMillis = expiresAtMillis - createdAtMillis;
        if (deltaMillis <= 0) {

            return 0;
        }

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

    @Override
    public void hydrate(@NotNull HydrationSink sink) {

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
        subscribe(TableNames.FRIENDS, FriendRow.class,
                row -> sink.applyFriend(toFriendLink(row)),
                row -> sink.removeFriend(row.id()));
        subscribe(TableNames.FRIEND_REQUESTS, FriendRequestRow.class,
                row -> sink.applyFriendRequest(toFriendRequest(row)),
                row -> sink.removeFriendRequest(row.id()));
        subscribe(TableNames.PARTIES, PartyRow.class,
                row -> sink.applyParty(toParty(row)),
                row -> sink.removeParty(row.id()));
        subscribe(TableNames.PARTY_MEMBERS, PartyMemberRow.class,
                row -> sink.applyPartyMember(toPartyMember(row)),
                row -> sink.removePartyMember(row.id()));
        subscribe(TableNames.MAIL, MailRow.class,
                row -> sink.applyMail(toMailMessage(row)),
                row -> sink.removeMail(row.id()));
        subscribe(TableNames.LOGIN_STREAKS, LoginStreakRow.class,
                row -> sink.applyLoginStreak(toLoginStreak(row)),
                row -> {});
        subscribe(TableNames.COSMETIC_GRANTS, net.mythicpvp.suite.database.schema.dto.CosmeticGrantRow.class,
                row -> sink.applyCosmeticGrant(UUID.fromString(row.player_uuid()), row.cosmetic_id(), row.cosmetic_type()),
                row -> {});
        subscribe(TableNames.COSMETIC_EQUIPPED, net.mythicpvp.suite.database.schema.dto.EquippedSlotRow.class,
                row -> sink.applyCosmeticEquip(UUID.fromString(row.player_uuid()), row.cosmetic_type(), row.cosmetic_id()),
                row -> {});
        logger.info("[stdb] hydration subscriptions registered for 13 tables");
    }

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
        D row = StdbRowParser.parse(event.payload(), dtoType);
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

    static long microsToMillis(long micros) {
        return micros == 0L ? 0L : micros / 1_000L;
    }

    @NotNull
    static FriendLink toFriendLink(@NotNull FriendRow row) {
        return new FriendLink(
                row.id(),
                UUID.fromString(row.owner_uuid()),
                UUID.fromString(row.friend_uuid()),
                microsToMillis(row.added_at()));
    }

    @NotNull
    static FriendRequest toFriendRequest(@NotNull FriendRequestRow row) {
        return new FriendRequest(
                row.id(),
                UUID.fromString(row.from_uuid()),
                UUID.fromString(row.to_uuid()),
                microsToMillis(row.created_at()));
    }

    @NotNull
    static Party toParty(@NotNull PartyRow row) {
        return new Party(row.id(), UUID.fromString(row.leader_uuid()), microsToMillis(row.created_at()));
    }

    @NotNull
    static PartyMember toPartyMember(@NotNull PartyMemberRow row) {
        return new PartyMember(
                row.id(),
                row.party_id(),
                UUID.fromString(row.player_uuid()),
                microsToMillis(row.joined_at()));
    }

    @NotNull
    static MailMessage toMailMessage(@NotNull MailRow row) {
        return new MailMessage(
                row.id(),
                UUID.fromString(row.recipient_uuid()),
                UUID.fromString(row.sender_uuid()),
                row.subject(),
                row.body(),
                row.attachments_json(),
                row.read(),
                microsToMillis(row.sent_at()),
                microsToMillis(row.read_at_micros()));
    }

    @NotNull
    static LoginStreak toLoginStreak(@NotNull LoginStreakRow row) {
        return new LoginStreak(
                row.id(),
                UUID.fromString(row.player_uuid()),
                microsToMillis(row.last_login_at()),
                row.current_streak());
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

package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PersistenceGateway {

    void rankDefine(@NotNull CoreRank rank, boolean seeded);

    void rankRemove(@NotNull String rankId);

    void grantIssue(@NotNull RankGrant grant);

    void grantDeactivate(long grantId);

    void grantRemoveInactive(long grantId);

    void grantClear(@NotNull UUID target);

    void punishIssue(@NotNull PunishmentRecord record);

    void punishPardon(long punishmentId, @NotNull UUID staff, @NotNull String reason);

    void punishClearHistory(@NotNull UUID target, @NotNull UUID staff);

    void templateUpsert(@NotNull PunishmentTemplate template, boolean seeded);

    void templateRemove(@NotNull String title);

    void blacklistAdd(@NotNull UUID target, @NotNull String targetName,
                      @NotNull UUID staff, @NotNull String staffName,
                      @NotNull String reason);

    void blacklistRevoke(long entryId, @NotNull UUID staff, @NotNull String reason);

    void appealOpen(long punishmentId, @NotNull UUID target, @NotNull String message);

    void appealReview(long appealId, @NotNull UUID reviewer, @NotNull String decision, @NotNull String notes);

    void cosmeticGrant(@NotNull UUID player,
                       @NotNull String cosmeticId,
                       @NotNull String cosmeticType,
                       @NotNull String source,
                       @NotNull String reference);

    void cosmeticEquip(@NotNull UUID player, @NotNull String cosmeticType, @NotNull String cosmeticId);

    void friendRequest(@NotNull UUID from, @NotNull UUID to);

    void friendAccept(long requestId);

    void friendDeny(long requestId);

    void friendRemove(@NotNull UUID owner, @NotNull UUID friend);

    void partyCreate(@NotNull UUID leader);

    void partyJoin(long partyId, @NotNull UUID player);

    void partyLeave(long partyId, @NotNull UUID player);

    void partyDisband(long partyId);

    void mailSend(@NotNull UUID sender, @NotNull UUID recipient,
                  @NotNull String subject, @NotNull String body,
                  @NotNull String attachmentsJson);

    void mailMarkRead(long mailId);

    void loginStreakRecord(@NotNull UUID player, long loginMillis, int streak);

    void hydrate(@NotNull HydrationSink sink);
}

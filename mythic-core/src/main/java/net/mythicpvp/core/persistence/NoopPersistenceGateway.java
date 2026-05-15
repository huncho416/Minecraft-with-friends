package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class NoopPersistenceGateway implements PersistenceGateway {

    public static final NoopPersistenceGateway INSTANCE = new NoopPersistenceGateway();

    private NoopPersistenceGateway() {
    }

    @Override public void rankDefine(@NotNull CoreRank rank, boolean seeded) {}
    @Override public void rankRemove(@NotNull String rankId) {}
    @Override public void grantIssue(@NotNull RankGrant grant) {}
    @Override public void grantDeactivate(long grantId) {}
    @Override public void grantRemoveInactive(long grantId) {}
    @Override public void grantClear(@NotNull UUID target) {}
    @Override public void punishIssue(@NotNull PunishmentRecord record) {}
    @Override public void punishPardon(long punishmentId, @NotNull UUID staff, @NotNull String reason) {}
    @Override public void punishClearHistory(@NotNull UUID target, @NotNull UUID staff) {}
    @Override public void templateUpsert(@NotNull PunishmentTemplate template, boolean seeded) {}
    @Override public void templateRemove(@NotNull String title) {}
    @Override public void blacklistAdd(@NotNull UUID target, @NotNull String targetName,
                                       @NotNull UUID staff, @NotNull String staffName,
                                       @NotNull String reason) {}
    @Override public void blacklistRevoke(long entryId, @NotNull UUID staff, @NotNull String reason) {}
    @Override public void appealOpen(long punishmentId, @NotNull UUID target, @NotNull String message) {}
    @Override public void appealReview(long appealId, @NotNull UUID reviewer, @NotNull String decision, @NotNull String notes) {}
    @Override public void cosmeticGrant(@NotNull UUID player, @NotNull String cosmeticId,
                                        @NotNull String cosmeticType, @NotNull String source,
                                        @NotNull String reference) {}
    @Override public void cosmeticEquip(@NotNull UUID player, @NotNull String cosmeticType, @NotNull String cosmeticId) {}
    @Override public void friendRequest(@NotNull UUID from, @NotNull UUID to) {}
    @Override public void friendAccept(long requestId) {}
    @Override public void friendDeny(long requestId) {}
    @Override public void friendRemove(@NotNull UUID owner, @NotNull UUID friend) {}
    @Override public void partyCreate(@NotNull UUID leader) {}
    @Override public void partyJoin(long partyId, @NotNull UUID player) {}
    @Override public void partyLeave(long partyId, @NotNull UUID player) {}
    @Override public void partyDisband(long partyId) {}
    @Override public void mailSend(@NotNull UUID sender, @NotNull UUID recipient,
                                   @NotNull String subject, @NotNull String body,
                                   @NotNull String attachmentsJson) {}
    @Override public void mailMarkRead(long mailId) {}
    @Override public void loginStreakRecord(@NotNull UUID player, long loginMillis, int streak) {}
    @Override public void hydrate(@NotNull HydrationSink sink) {}
}

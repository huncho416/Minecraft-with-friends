package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test gateway that records every call so persistence-path assertions
 * can be made without bringing up a real STDB connection.
 *
 * <p>Stores call records as inner-record types so tests can pattern-match
 * cleanly. Order is preserved.
 */
public final class CapturingPersistenceGateway implements PersistenceGateway {

    public final List<Object> calls = new ArrayList<>();

    public record RankDefine(CoreRank rank, boolean seeded) {}
    public record RankRemove(String rankId) {}
    public record GrantIssue(RankGrant grant) {}
    public record GrantDeactivate(long grantId) {}
    public record GrantRemoveInactive(long grantId) {}
    public record GrantClear(UUID target) {}
    public record PunishIssue(PunishmentRecord record) {}
    public record PunishPardon(long punishmentId, UUID staff, String reason) {}
    public record PunishClearHistory(UUID target, UUID staff) {}
    public record TemplateUpsert(PunishmentTemplate template, boolean seeded) {}
    public record TemplateRemove(String title) {}
    public record BlacklistAdd(UUID target, String targetName, UUID staff, String staffName, String reason) {}
    public record BlacklistRevoke(long entryId, UUID staff, String reason) {}

    @Override public void rankDefine(@NotNull CoreRank rank, boolean seeded) { calls.add(new RankDefine(rank, seeded)); }
    @Override public void rankRemove(@NotNull String rankId) { calls.add(new RankRemove(rankId)); }
    @Override public void grantIssue(@NotNull RankGrant grant) { calls.add(new GrantIssue(grant)); }
    @Override public void grantDeactivate(long grantId) { calls.add(new GrantDeactivate(grantId)); }
    @Override public void grantRemoveInactive(long grantId) { calls.add(new GrantRemoveInactive(grantId)); }
    @Override public void grantClear(@NotNull UUID target) { calls.add(new GrantClear(target)); }
    @Override public void punishIssue(@NotNull PunishmentRecord record) { calls.add(new PunishIssue(record)); }
    @Override public void punishPardon(long punishmentId, @NotNull UUID staff, @NotNull String reason) { calls.add(new PunishPardon(punishmentId, staff, reason)); }
    @Override public void punishClearHistory(@NotNull UUID target, @NotNull UUID staff) { calls.add(new PunishClearHistory(target, staff)); }
    @Override public void templateUpsert(@NotNull PunishmentTemplate template, boolean seeded) { calls.add(new TemplateUpsert(template, seeded)); }
    @Override public void templateRemove(@NotNull String title) { calls.add(new TemplateRemove(title)); }
    @Override public void blacklistAdd(@NotNull UUID target, @NotNull String targetName,
                                       @NotNull UUID staff, @NotNull String staffName,
                                       @NotNull String reason) {
        calls.add(new BlacklistAdd(target, targetName, staff, staffName, reason));
    }
    @Override public void blacklistRevoke(long entryId, @NotNull UUID staff, @NotNull String reason) {
        calls.add(new BlacklistRevoke(entryId, staff, reason));
    }
    public record AppealOpen(long punishmentId, UUID target, String message) {}
    public record AppealReview(long appealId, UUID reviewer, String decision, String notes) {}
    @Override public void appealOpen(long punishmentId, @NotNull UUID target, @NotNull String message) {
        calls.add(new AppealOpen(punishmentId, target, message));
    }
    @Override public void appealReview(long appealId, @NotNull UUID reviewer, @NotNull String decision, @NotNull String notes) {
        calls.add(new AppealReview(appealId, reviewer, decision, notes));
    }
    public record CosmeticGrant(UUID player, String cosmeticId, String cosmeticType, String source, String reference) {}
    @Override public void cosmeticGrant(@NotNull UUID player, @NotNull String cosmeticId,
                                        @NotNull String cosmeticType, @NotNull String source,
                                        @NotNull String reference) {
        calls.add(new CosmeticGrant(player, cosmeticId, cosmeticType, source, reference));
    }
    /** Records the hydrate invocation and the sink so tests can confirm wiring. */
    public record HydrateCall(HydrationSink sink) {}
    @Override public void hydrate(@NotNull HydrationSink sink) {
        calls.add(new HydrateCall(sink));
    }
}

package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * No-op gateway used in tests and in single-server / standalone runs
 * where SpacetimeDB isn't reachable. Every method is a successful no-op.
 *
 * <p>Singleton: there's no per-instance state.
 */
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
}

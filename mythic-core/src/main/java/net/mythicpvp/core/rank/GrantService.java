package net.mythicpvp.core.rank;

import net.mythicpvp.suite.permission.PermissionManager;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class GrantService {

    private final RankService rankService;
    private final Clock clock;
    private final AtomicLong ids = new AtomicLong();
    private final List<RankGrant> grants = new CopyOnWriteArrayList<>();

    public GrantService(@NotNull RankService rankService, @NotNull Clock clock) {
        this.rankService = rankService;
        this.clock = clock;
    }

    @NotNull
    public RankGrant grant(@NotNull UUID targetUuid, @NotNull String targetName, @NotNull String rankId, @NotNull GrantDuration duration, @NotNull UUID executorUuid, @NotNull String executorName, @NotNull String reason) {
        CoreRank rank = rankService.get(rankId);
        if (rank == null) {
            throw new IllegalArgumentException("rank");
        }
        long now = clock.millis();
        RankGrant grant = new RankGrant(ids.incrementAndGet(), targetUuid, targetName, rank.id(), executorUuid, executorName, reason, now, duration.expiresAt(now), true);
        grants.add(grant);
        PermissionManager.getInstance().setPlayerRank(targetUuid, activeRank(targetUuid));
        return grant;
    }

    @NotNull
    public List<RankGrant> history(@NotNull UUID targetUuid) {
        return grants.stream()
                .filter(grant -> grant.targetUuid().equals(targetUuid))
                .sorted(Comparator.comparingLong(RankGrant::createdAtMillis).reversed())
                .toList();
    }

    @NotNull
    public List<RankGrant> active(@NotNull UUID targetUuid) {
        long now = clock.millis();
        return history(targetUuid).stream()
                .filter(RankGrant::active)
                .filter(grant -> !grant.expired(now))
                .toList();
    }

    public boolean deactivate(long grantId) {
        for (RankGrant grant : grants) {
            if (grant.id() == grantId && grant.active()) {
                grants.remove(grant);
                grants.add(new RankGrant(grant.id(), grant.targetUuid(), grant.targetName(), grant.rankId(), grant.executorUuid(), grant.executorName(), grant.reason(), grant.createdAtMillis(), grant.expiresAtMillis(), false));
                PermissionManager.getInstance().setPlayerRank(grant.targetUuid(), activeRank(grant.targetUuid()));
                return true;
            }
        }
        return false;
    }

    public boolean removeInactive(long grantId) {
        for (RankGrant grant : grants) {
            if (grant.id() == grantId && !grant.active()) {
                return grants.remove(grant);
            }
        }
        return false;
    }

    public int clear(@NotNull UUID targetUuid) {
        int before = grants.size();
        grants.removeIf(grant -> grant.targetUuid().equals(targetUuid));
        PermissionManager.getInstance().setPlayerRank(targetUuid, "default");
        return before - grants.size();
    }

    @NotNull
    public String activeRank(@NotNull UUID targetUuid) {
        return active(targetUuid).stream()
                .map(grant -> rankService.get(grant.rankId()))
                .filter(rank -> rank != null)
                .min(Comparator.comparingInt(CoreRank::weight))
                .map(CoreRank::id)
                .orElse("default");
    }
}

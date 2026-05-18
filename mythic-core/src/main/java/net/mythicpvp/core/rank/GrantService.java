package net.mythicpvp.core.rank;

import net.mythicpvp.core.persistence.NoopPersistenceGateway;
import net.mythicpvp.core.persistence.PersistenceGateway;
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
    private volatile String localNetworkType = CoreRank.SCOPE_GLOBAL;

    private volatile PersistenceGateway persistence = NoopPersistenceGateway.INSTANCE;

    private volatile java.util.function.Consumer<UUID> displayRefresher = uuid -> {};

    private volatile java.util.function.Consumer<RankGrant> grantObserver = grant -> {};

    public GrantService(@NotNull RankService rankService, @NotNull Clock clock) {
        this.rankService = rankService;
        this.clock = clock;
    }

    public void setLocalNetworkType(@NotNull String networkType) {
        this.localNetworkType = networkType.isBlank() ? CoreRank.SCOPE_GLOBAL : networkType;
    }

    public void setPersistence(@NotNull PersistenceGateway persistence) {
        this.persistence = persistence;
    }

    public void setDisplayRefresher(@NotNull java.util.function.Consumer<UUID> refresher) {
        this.displayRefresher = refresher;
    }

    public void setGrantObserver(@NotNull java.util.function.Consumer<RankGrant> observer) {
        this.grantObserver = observer;
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
        persistence.grantIssue(grant);
        displayRefresher.accept(targetUuid);

        grantObserver.accept(grant);
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
                persistence.grantDeactivate(grantId);
                displayRefresher.accept(grant.targetUuid());
                return true;
            }
        }
        return false;
    }

    public boolean removeInactive(long grantId) {
        for (RankGrant grant : grants) {
            if (grant.id() == grantId && !grant.active()) {
                boolean removed = grants.remove(grant);
                if (removed) {
                    persistence.grantRemoveInactive(grantId);
                }
                return removed;
            }
        }
        return false;
    }

    public int clear(@NotNull UUID targetUuid) {
        int before = grants.size();
        grants.removeIf(grant -> grant.targetUuid().equals(targetUuid));
        PermissionManager.getInstance().setPlayerRank(targetUuid, "default");
        int removed = before - grants.size();
        if (removed > 0) {
            persistence.grantClear(targetUuid);
            displayRefresher.accept(targetUuid);
        }
        return removed;
    }

    public void applyGrant(@NotNull RankGrant grant) {
        RankGrant existing = grants.stream()
                .filter(g -> g.id() == grant.id())
                .findFirst()
                .orElse(null);
        boolean unchanged = existing != null && existing.equals(grant);
        if (existing != null) {
            grants.remove(existing);
        }
        grants.add(grant);

        long observed = grant.id();
        long current = ids.get();
        if (observed > current) {
            ids.compareAndSet(current, observed);
        }
        if (unchanged) {
            return;
        }
        PermissionManager.getInstance().setPlayerRank(grant.targetUuid(), activeRank(grant.targetUuid()));
        displayRefresher.accept(grant.targetUuid());
    }

    public void removeGrant(long grantId) {
        UUID owner = grants.stream()
                .filter(g -> g.id() == grantId)
                .map(RankGrant::targetUuid)
                .findFirst()
                .orElse(null);
        grants.removeIf(g -> g.id() == grantId);
        if (owner != null) {
            PermissionManager.getInstance().setPlayerRank(owner, activeRank(owner));
            displayRefresher.accept(owner);
        }
    }

    @NotNull
    public String activeRank(@NotNull UUID targetUuid) {
        String network = localNetworkType;
        return active(targetUuid).stream()
                .map(grant -> rankService.get(grant.rankId()))
                .filter(rank -> rank != null)
                .filter(rank -> rank.matchesNetwork(network))
                .max(Comparator.comparingInt(CoreRank::weight))
                .map(CoreRank::id)
                .orElse("default");
    }
}

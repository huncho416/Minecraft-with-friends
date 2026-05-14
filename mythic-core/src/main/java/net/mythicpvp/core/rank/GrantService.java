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
    // Optional persistence sink. Defaults to no-op so existing tests stay
    // green; production wiring sets this to StdbPersistenceGateway in
    // MythicCorePlugin.onEnable.
    private volatile PersistenceGateway persistence = NoopPersistenceGateway.INSTANCE;
    // Optional display refresher. Called with the affected player's UUID
    // after every grant mutation so the DisplayService can re-push tab,
    // nametag, and scoreboard for that player. No-op in tests.
    private volatile java.util.function.Consumer<UUID> displayRefresher = uuid -> {};
    // Optional grant observer — fires once after a successful grant().
    // Used by the cosmetic-bundle integration to auto-grant the rank's
    // bundled cosmetics. No-op in tests.
    private volatile java.util.function.Consumer<RankGrant> grantObserver = grant -> {};

    public GrantService(@NotNull RankService rankService, @NotNull Clock clock) {
        this.rankService = rankService;
        this.clock = clock;
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
        // Fire the post-grant observer last so persistence + display
        // are already in the right state if the observer reads them.
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

    /**
     * Insert-or-replace a grant by id without firing the persistence
     * gateway. Used by the STDB hydration path when rows arrive from the
     * database; calling {@link #grant} would re-write the same row to
     * STDB and infinite-loop.
     *
     * <p>Also keeps the auto-inc id generator monotonically ahead of any
     * id observed from STDB so future {@link #grant} calls don't collide.
     */
    public void applyGrant(@NotNull RankGrant grant) {
        grants.removeIf(existing -> existing.id() == grant.id());
        grants.add(grant);
        // Keep the id generator ahead of any seen id so subsequent
        // grant() calls produce unique ids.
        long observed = grant.id();
        long current = ids.get();
        if (observed > current) {
            ids.compareAndSet(current, observed);
        }
        PermissionManager.getInstance().setPlayerRank(grant.targetUuid(), activeRank(grant.targetUuid()));
        // Hydration also touches the visible rank, so refresh display.
        // Refresher schedules onto the main thread itself so this is
        // safe to call from the STDB driver thread via the sink.
        displayRefresher.accept(grant.targetUuid());
    }

    /** Remove a grant by id without firing the persistence gateway. */
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
        return active(targetUuid).stream()
                .map(grant -> rankService.get(grant.rankId()))
                .filter(rank -> rank != null)
                .min(Comparator.comparingInt(CoreRank::weight))
                .map(CoreRank::id)
                .orElse("default");
    }
}

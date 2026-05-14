package net.mythicpvp.core.cosmetic;

import net.mythicpvp.core.audit.CoreAuditLog;
import net.mythicpvp.core.persistence.PersistenceGateway;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * {@link Consumer} attached to {@link
 * net.mythicpvp.core.rank.GrantService#setGrantObserver} — auto-grants
 * the bundled cosmetics for the just-granted rank.
 *
 * <p>The bundle map lives in {@link RankCosmeticBundles} (loaded from
 * ranks.yml). For each bundled cosmetic id we call
 * {@link CosmeticManager#grantCosmetic} for the local-runtime grant +
 * {@link PersistenceGateway} for the persistence path. Today the
 * persistence call is a placeholder because {@code cosmetic_grant}
 * needs the cosmetic's type, which isn't on the rank — so we log the
 * intent to the audit log and skip STDB until the catalog hydrates.
 *
 * <p>Audit log entries: {@code COSMETIC_BUNDLE_GRANT} per cosmetic.
 */
public final class RankBundleGrantHook implements Consumer<RankGrant> {

    private final RankCosmeticBundles bundles;
    private final CoreAuditLog audit;
    private final Logger logger;
    private final PersistenceGateway persistence;

    public RankBundleGrantHook(
            @NotNull RankCosmeticBundles bundles,
            @NotNull CoreAuditLog audit,
            @NotNull Logger logger) {
        // Test-friendly overload — defaults to no STDB persistence so
        // existing fixtures don't need to wire a gateway.
        this(bundles, audit, logger,
                net.mythicpvp.core.persistence.NoopPersistenceGateway.INSTANCE);
    }

    public RankBundleGrantHook(
            @NotNull RankCosmeticBundles bundles,
            @NotNull CoreAuditLog audit,
            @NotNull Logger logger,
            @NotNull PersistenceGateway persistence) {
        this.bundles = bundles;
        this.audit = audit;
        this.logger = logger;
        this.persistence = persistence;
    }

    @Override
    public void accept(@NotNull RankGrant grant) {
        List<String> bundled = bundles.bundledFor(grant.rankId());
        if (bundled.isEmpty()) {
            return;
        }
        for (String cosmeticId : bundled) {
            CosmeticManager.Cosmetic cosmetic = CosmeticManager.getInstance().get(cosmeticId);
            try {
                CosmeticManager.getInstance().grantCosmetic(grant.targetUuid(), cosmeticId);
            } catch (RuntimeException e) {
                // Non-fatal — a missing cosmetic shouldn't kill the
                // grant. Log + continue with the rest of the bundle.
                logger.warning("[cosmetic-bundle] grant " + cosmeticId
                        + " to " + grant.targetUuid() + " failed: " + e.getMessage());
                continue;
            }
            audit.log("COSMETIC_BUNDLE_GRANT",
                    grant.executorUuid(), grant.executorName(),
                    grant.targetUuid(), grant.targetName(),
                    Map.of(
                            "rank", grant.rankId(),
                            "cosmetic", cosmeticId,
                            "grant_id", Long.toString(grant.id())));
            // Persist to STDB once we know the cosmetic's type. If the
            // catalog hasn't hydrated yet we can't translate the gameplay
            // type → STDB wire enum, so we skip the persistence call and
            // rely on the audit log + local runtime grant for now. The
            // next bundle grant after catalog load will succeed.
            if (cosmetic != null) {
                persistence.cosmeticGrant(
                        grant.targetUuid(),
                        cosmeticId,
                        cosmetic.type().name(),
                        "RANK_BUNDLE",
                        grant.rankId());
            }
        }
    }
}

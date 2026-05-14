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

public final class RankBundleGrantHook implements Consumer<RankGrant> {

    private final RankCosmeticBundles bundles;
    private final CoreAuditLog audit;
    private final Logger logger;
    private final PersistenceGateway persistence;

    public RankBundleGrantHook(
            @NotNull RankCosmeticBundles bundles,
            @NotNull CoreAuditLog audit,
            @NotNull Logger logger) {

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

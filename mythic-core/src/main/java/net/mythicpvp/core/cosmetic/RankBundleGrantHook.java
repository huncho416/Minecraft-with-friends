package net.mythicpvp.core.cosmetic;

import net.mythicpvp.core.audit.CoreAuditLog;
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

    public RankBundleGrantHook(
            @NotNull RankCosmeticBundles bundles,
            @NotNull CoreAuditLog audit,
            @NotNull Logger logger) {
        this.bundles = bundles;
        this.audit = audit;
        this.logger = logger;
    }

    @Override
    public void accept(@NotNull RankGrant grant) {
        List<String> bundled = bundles.bundledFor(grant.rankId());
        if (bundled.isEmpty()) {
            return;
        }
        for (String cosmeticId : bundled) {
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
        }
    }
}

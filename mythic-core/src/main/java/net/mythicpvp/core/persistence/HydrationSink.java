package net.mythicpvp.core.persistence;

import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentTemplate;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankGrant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface HydrationSink {

    void applyRank(@NotNull CoreRank rank);

    void removeRank(@NotNull String rankId);

    void applyGrant(@NotNull RankGrant grant);

    void removeGrant(long grantId);

    void applyPunishment(@NotNull PunishmentRecord record);

    void removePunishment(long punishmentId);

    void applyTemplate(@NotNull PunishmentTemplate template);

    void removeTemplate(@NotNull String title);

    void applyBlacklist(@NotNull UUID target, @NotNull String targetName, boolean active);
}

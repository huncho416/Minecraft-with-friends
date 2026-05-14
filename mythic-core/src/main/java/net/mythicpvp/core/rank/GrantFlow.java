package net.mythicpvp.core.rank;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record GrantFlow(
        @NotNull UUID targetUuid,
        @NotNull String targetName,
        String rankId,
        GrantDuration duration,
        String reason
) {
    @NotNull
    public GrantFlow rank(@NotNull String nextRank) {
        return new GrantFlow(targetUuid, targetName, nextRank, duration, reason);
    }

    @NotNull
    public GrantFlow duration(@NotNull GrantDuration nextDuration) {
        return new GrantFlow(targetUuid, targetName, rankId, nextDuration, reason);
    }

    @NotNull
    public GrantFlow reason(@NotNull String nextReason) {
        return new GrantFlow(targetUuid, targetName, rankId, duration, nextReason);
    }
}

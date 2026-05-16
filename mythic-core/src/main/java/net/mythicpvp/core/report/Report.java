package net.mythicpvp.core.report;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Report {

    private final long id;
    private final UUID reporterUuid;
    private final String reporterName;
    private final UUID targetUuid;
    private final String targetName;
    private final ReportCategory category;
    private final String reporterServer;
    private final long submittedAt;

    private volatile boolean resolved;
    private volatile UUID resolverUuid;
    private volatile String resolverName;
    private volatile String resolution;
    private volatile long resolvedAt;
    private volatile String targetServerCache;

    public Report(long id,
                  @NotNull UUID reporterUuid,
                  @NotNull String reporterName,
                  @NotNull UUID targetUuid,
                  @NotNull String targetName,
                  @NotNull ReportCategory category,
                  @NotNull String reporterServer,
                  long submittedAt) {
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.category = category;
        this.reporterServer = reporterServer;
        this.submittedAt = submittedAt;
    }

    public long id() {
        return id;
    }

    @NotNull
    public UUID reporterUuid() {
        return reporterUuid;
    }

    @NotNull
    public String reporterName() {
        return reporterName;
    }

    @NotNull
    public UUID targetUuid() {
        return targetUuid;
    }

    @NotNull
    public String targetName() {
        return targetName;
    }

    @NotNull
    public ReportCategory category() {
        return category;
    }

    @NotNull
    public String reporterServer() {
        return reporterServer;
    }

    public long submittedAt() {
        return submittedAt;
    }

    public boolean resolved() {
        return resolved;
    }

    @Nullable
    public UUID resolverUuid() {
        return resolverUuid;
    }

    @Nullable
    public String resolverName() {
        return resolverName;
    }

    @Nullable
    public String resolution() {
        return resolution;
    }

    public long resolvedAt() {
        return resolvedAt;
    }

    @Nullable
    public String targetServerCache() {
        return targetServerCache;
    }

    public void setTargetServerCache(@Nullable String value) {
        this.targetServerCache = value;
    }

    public void markResolved(@NotNull UUID resolverUuid, @NotNull String resolverName,
                             @NotNull String resolution, long resolvedAt) {
        this.resolved = true;
        this.resolverUuid = resolverUuid;
        this.resolverName = resolverName;
        this.resolution = resolution;
        this.resolvedAt = resolvedAt;
    }
}

package net.mythicpvp.core.report;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ReportService {

    private final Map<Long, Report> reports = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private volatile ReportStore store;

    public void setStore(@NotNull ReportStore store) {
        this.store = store;
        ReportStore.LoadResult loaded = store.load();
        for (Report report : loaded.reports()) {
            reports.put(report.id(), report);
        }
        sequence.set(Math.max(sequence.get(), loaded.maxId()));
    }

    @NotNull
    public Report submit(@NotNull UUID reporterUuid,
                         @NotNull String reporterName,
                         @NotNull UUID targetUuid,
                         @NotNull String targetName,
                         @NotNull ReportCategory category,
                         @NotNull String reporterServer) {
        long id = sequence.incrementAndGet();
        Report report = new Report(id, reporterUuid, reporterName, targetUuid, targetName,
                category, reporterServer, System.currentTimeMillis());
        reports.put(id, report);
        flush();
        return report;
    }

    @Nullable
    public Report get(long id) {
        return reports.get(id);
    }

    @NotNull
    public List<Report> active() {
        List<Report> result = new ArrayList<>();
        for (Report r : reports.values()) {
            if (!r.resolved()) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingLong(Report::submittedAt).reversed());
        return result;
    }

    @NotNull
    public List<Report> resolved() {
        List<Report> result = new ArrayList<>();
        for (Report r : reports.values()) {
            if (r.resolved()) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingLong(Report::resolvedAt).reversed());
        return result;
    }

    public boolean delete(long id) {
        boolean removed = reports.remove(id) != null;
        if (removed) flush();
        return removed;
    }

    public boolean resolve(long id, @NotNull UUID resolverUuid, @NotNull String resolverName,
                           @NotNull String resolution) {
        Report report = reports.get(id);
        if (report == null || report.resolved()) {
            return false;
        }
        report.markResolved(resolverUuid, resolverName, resolution, System.currentTimeMillis());
        flush();
        return true;
    }

    public void flush() {
        if (store != null) {
            store.save(sequence.get(), new ArrayList<>(reports.values()));
        }
    }
}

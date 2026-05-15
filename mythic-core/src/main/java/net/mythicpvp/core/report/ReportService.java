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
        return reports.remove(id) != null;
    }
}

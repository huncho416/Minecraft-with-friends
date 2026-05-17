package net.mythicpvp.core.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.StdbRowParser;
import net.mythicpvp.suite.database.schema.TableNames;
import net.mythicpvp.suite.database.schema.dto.ReportRow;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReportSqlRefresher {

    private final ReportService reports;
    private final Logger logger;
    private final Set<Long> seenIds = new HashSet<>();
    private volatile boolean baselineDone = false;
    private volatile Consumer<Report> onNewReport = r -> {};
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mythic-report-refresh");
        t.setDaemon(true);
        return t;
    });

    public ReportSqlRefresher(@NotNull ReportService reports, @NotNull Logger logger) {
        this.reports = reports;
        this.logger = logger;
    }

    public void setNewReportListener(@NotNull Consumer<Report> listener) {
        this.onNewReport = listener;
    }

    public void start() {
        SpacetimeConnection connection;
        try {
            connection = DatabaseManager.getInstance().getPrimary();
        } catch (IllegalStateException e) {
            logger.info("[report-refresh] no STDB connection; cross-shard reports disabled");
            return;
        }
        executor.scheduleAtFixedRate(() -> poll(connection), 2, 5, TimeUnit.SECONDS);
        logger.info("[report-refresh] polling reports every 5s");
    }

    private void poll(@NotNull SpacetimeConnection connection) {
        connection.sql("SELECT * FROM " + TableNames.REPORTS).thenAccept(body -> {
            try {
                apply(body);
            } catch (RuntimeException e) {
                logger.log(Level.FINE, "[report-refresh] parse failed", e);
            }
        });
    }

    private void apply(@NotNull String body) {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException e) {
            return;
        }
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) return;
        JsonObject table = root.getAsJsonArray().get(0).getAsJsonObject();
        if (!table.has("rows")) return;
        JsonArray rows = table.getAsJsonArray("rows");

        Set<Long> currentIds = new HashSet<>();
        for (JsonElement rowEl : rows) {
            if (!rowEl.isJsonArray()) continue;
            ReportRow row = StdbRowParser.parse(rowEl.toString(), ReportRow.class);
            if (row == null) continue;
            currentIds.add(row.id());
            UUID reporterUuid;
            UUID targetUuid;
            try {
                reporterUuid = UUID.fromString(row.reporter_uuid());
                targetUuid = UUID.fromString(row.target_uuid());
            } catch (RuntimeException e) {
                continue;
            }
            ReportCategory category;
            try {
                category = ReportCategory.valueOf(row.category());
            } catch (IllegalArgumentException e) {
                continue;
            }
            Report report = new Report(row.id(), reporterUuid, row.reporter_name(),
                    targetUuid, row.target_name(), category,
                    row.reporter_shard(), row.created_at() / 1000L);
            if (row.resolved()) {
                report.markResolved(
                        UUID.fromString(row.resolver_uuid().isBlank()
                                ? "00000000-0000-0000-0000-000000000000"
                                : row.resolver_uuid()),
                        row.resolver_name(),
                        row.resolution(),
                        row.resolved_at_micros() / 1000L);
            }
            reports.applyRemote(report);
            if (baselineDone && !seenIds.contains(row.id()) && !row.resolved()) {
                onNewReport.accept(report);
            }
        }
        seenIds.clear();
        seenIds.addAll(currentIds);
        baselineDone = true;
    }
}

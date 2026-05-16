package net.mythicpvp.core.report;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReportStore {

    private final File file;
    private final Logger logger;

    public ReportStore(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @NotNull
    public LoadResult load() {
        if (!file.exists()) {
            return new LoadResult(List.of(), 0L);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        long maxId = yaml.getLong("sequence", 0L);
        List<Report> reports = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("reports");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection r = section.getConfigurationSection(key);
                if (r == null) continue;
                try {
                    Report report = readReport(r);
                    reports.add(report);
                    if (report.id() > maxId) maxId = report.id();
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Skipping malformed report entry " + key, e);
                }
            }
        }
        return new LoadResult(reports, maxId);
    }

    public void save(long sequence, @NotNull List<Report> reports) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("sequence", sequence);
        for (Report report : reports) {
            String prefix = "reports." + report.id() + ".";
            yaml.set(prefix + "reporterUuid", report.reporterUuid().toString());
            yaml.set(prefix + "reporterName", report.reporterName());
            yaml.set(prefix + "targetUuid", report.targetUuid().toString());
            yaml.set(prefix + "targetName", report.targetName());
            yaml.set(prefix + "category", report.category().name());
            yaml.set(prefix + "reporterServer", report.reporterServer());
            yaml.set(prefix + "submittedAt", report.submittedAt());
            yaml.set(prefix + "resolved", report.resolved());
            if (report.resolved()) {
                yaml.set(prefix + "resolverUuid",
                        report.resolverUuid() == null ? null : report.resolverUuid().toString());
                yaml.set(prefix + "resolverName", report.resolverName());
                yaml.set(prefix + "resolution", report.resolution());
                yaml.set(prefix + "resolvedAt", report.resolvedAt());
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save reports.yml", e);
        }
    }

    @NotNull
    private static Report readReport(@NotNull ConfigurationSection r) {
        long id = Long.parseLong(r.getName());
        UUID reporterUuid = UUID.fromString(r.getString("reporterUuid"));
        String reporterName = r.getString("reporterName", "?");
        UUID targetUuid = UUID.fromString(r.getString("targetUuid"));
        String targetName = r.getString("targetName", "?");
        ReportCategory category = ReportCategory.valueOf(r.getString("category", "OTHER"));
        String reporterServer = r.getString("reporterServer", "?");
        long submittedAt = r.getLong("submittedAt");
        Report report = new Report(id, reporterUuid, reporterName, targetUuid, targetName,
                category, reporterServer, submittedAt);
        if (r.getBoolean("resolved", false)) {
            UUID resolverUuid = r.contains("resolverUuid")
                    ? UUID.fromString(r.getString("resolverUuid")) : reporterUuid;
            String resolverName = r.getString("resolverName", "?");
            String resolution = r.getString("resolution", "");
            long resolvedAt = r.getLong("resolvedAt");
            report.markResolved(resolverUuid, resolverName, resolution, resolvedAt);
        }
        return report;
    }

    public record LoadResult(@NotNull List<Report> reports, long maxId) {
    }
}

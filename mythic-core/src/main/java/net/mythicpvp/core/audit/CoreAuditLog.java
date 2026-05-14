package net.mythicpvp.core.audit;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CoreAuditLog {

    private static final java.time.format.DateTimeFormatter ISO_UTC =
            java.time.format.DateTimeFormatter.ISO_INSTANT;

    private final Logger logger;
    private final Path file;

    public CoreAuditLog(@NotNull Plugin plugin) {
        this.logger = plugin.getLogger();
        this.file = plugin.getDataFolder().toPath().resolve("audit.log");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            logger.log(Level.WARNING, "could not create audit.log parent: " + e.getMessage());
        }
    }

    public void log(
            @NotNull String action,
            UUID staff,
            @NotNull String staffName,
            UUID target,
            @NotNull String targetName,
            @NotNull Map<String, String> details) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(ISO_UTC.format(Instant.now()))
                .append(" [").append(action.toUpperCase(Locale.ROOT)).append("] ")
                .append("staff=").append(escapeValue(staffName))
                .append('/').append(staff == null ? "-" : staff)
                .append(" target=").append(escapeValue(targetName))
                .append('/').append(target == null ? "-" : target);
        if (!details.isEmpty()) {
            sb.append(" details=");
            boolean first = true;
            for (Map.Entry<String, String> entry : details.entrySet()) {
                if (!first) sb.append(',');
                sb.append(entry.getKey()).append('=').append(escapeValue(entry.getValue()));
                first = false;
            }
        }
        write(sb.toString());
    }

    private void write(@NotNull String line) {
        try (Writer writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(writer)) {
            pw.println(line);
        } catch (IOException e) {
            logger.log(Level.WARNING, "audit-log write failed: " + e.getMessage());
        }
    }

    @NotNull
    private static String escapeValue(@NotNull String value) {
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(',', ';')
                .replace('=', '_');
    }
}

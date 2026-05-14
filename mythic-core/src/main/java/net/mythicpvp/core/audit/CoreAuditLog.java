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

/**
 * Append-only structured audit log for staff actions.
 *
 * <p>Writes one line per event into {@code plugins/MythicCore/audit.log},
 * shape:
 *
 * <pre>{@code
 *   2026-05-14T12:34:56Z [GRANT] staff=Admin/22…22 target=Notch/11…11 details=rank=vip duration=30d
 * }</pre>
 *
 * <p>Format choice: line-per-event makes it grep-friendly; the
 * key=value detail block survives shell munging better than CSV; ISO-8601
 * UTC timestamps sort lexicographically. Not JSON because ops grep, and
 * JSON-per-line bloats the file 3x for the same useful content.
 *
 * <p>Failures are logged via the plugin's {@link Logger} — audit-log
 * write errors must never cascade back into the gameplay path.
 */
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

    /**
     * Record a staff action.
     *
     * @param action  short uppercase verb — GRANT, PARDON, BLACKLIST_ADD, etc.
     * @param staff   actor uuid — pass {@code null} for system-driven events
     * @param staffName  actor display name (or "SYSTEM")
     * @param target  affected player uuid (nullable for global actions)
     * @param targetName  affected player name
     * @param details key=value pairs flattened into the line
     */
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

    /**
     * Strip newlines/spaces/quotes from a value so the line stays
     * parseable. Audit values shouldn't contain these in practice, but
     * a malicious reason text could try to forge a fake log entry.
     */
    @NotNull
    private static String escapeValue(@NotNull String value) {
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(',', ';')
                .replace('=', '_');
    }
}

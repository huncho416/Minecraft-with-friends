package net.mythicpvp.suite.database.schema;

import net.mythicpvp.suite.database.SpacetimeConnection;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Schema version pinned to {@code mythic-stdb}'s {@code SCHEMA_VERSION}.
 *
 * <p>Bump this in lockstep with {@code mythic-cord/stdb/src/lib.rs}. The
 * server refuses to start when this constant disagrees with the live STDB
 * {@code module_meta} row.
 */
public final class SchemaVersion {

    /** Must equal {@code SCHEMA_VERSION} in {@code mythic-cord/stdb/src/lib.rs}. */
    public static final int CURRENT = 1;

    private SchemaVersion() {}

    /**
     * Block-on (with timeout) verification that the STDB module is publishing
     * a schema we understand. Call this once during plugin enable, before
     * any subscriptions are wired.
     *
     * @throws SchemaVersionMismatchException when versions disagree or the
     *         lookup times out — caller should refuse to start.
     */
    public static void assertMatches(@NotNull SpacetimeConnection connection)
            throws SchemaVersionMismatchException {
        try {
            CompletableFuture<Integer> remote = new CompletableFuture<>();
            connection.subscribeTable(TableNames.MODULE_META, event -> {
                Integer parsed = parseSchemaVersion(event.payload());
                if (parsed != null && !remote.isDone()) {
                    remote.complete(parsed);
                }
            });
            int actual = remote.get(10, TimeUnit.SECONDS);
            if (actual != CURRENT) {
                throw new SchemaVersionMismatchException(CURRENT, actual);
            }
        } catch (TimeoutException timeout) {
            throw new SchemaVersionMismatchException(CURRENT, -1);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new SchemaVersionMismatchException(CURRENT, -1);
        } catch (Exception other) {
            throw new SchemaVersionMismatchException(CURRENT, -1);
        }
    }

    /**
     * Extract {@code schema_version} from a JSON payload. Returns
     * {@code null} when the field is absent — caller treats that as a
     * still-loading row.
     */
    static Integer parseSchemaVersion(@NotNull String payload) {
        int index = payload.indexOf("schema_version");
        if (index < 0) {
            return null;
        }
        int colon = payload.indexOf(':', index);
        if (colon < 0) {
            return null;
        }
        int start = colon + 1;
        while (start < payload.length() && Character.isWhitespace(payload.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < payload.length() && Character.isDigit(payload.charAt(end))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        return Integer.parseInt(payload.substring(start, end));
    }

    /** Thrown when STDB schema version disagrees with {@link #CURRENT}. */
    public static final class SchemaVersionMismatchException extends Exception {
        private final int expected;
        private final int actual;

        SchemaVersionMismatchException(int expected, int actual) {
            super("STDB schema version mismatch: suite expects " + expected
                    + ", module reports " + (actual < 0 ? "<unknown>" : actual)
                    + ". Republish mythic-stdb or update the suite.");
            this.expected = expected;
            this.actual = actual;
        }

        public int expected() { return expected; }
        public int actual() { return actual; }
    }
}

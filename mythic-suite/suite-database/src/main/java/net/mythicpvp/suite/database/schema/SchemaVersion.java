package net.mythicpvp.suite.database.schema;

import net.mythicpvp.suite.database.SpacetimeConnection;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SchemaVersion {

    public static final int CURRENT = 2;

    private SchemaVersion() {}

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

    public static final class SchemaVersionMismatchException extends Exception {
        private static final long serialVersionUID = 1L;
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

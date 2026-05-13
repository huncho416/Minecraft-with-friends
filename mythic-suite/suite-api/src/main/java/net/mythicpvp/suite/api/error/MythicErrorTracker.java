package net.mythicpvp.suite.api.error;

import io.sentry.Sentry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MythicErrorTracker {

    private static volatile boolean initialized;

    private MythicErrorTracker() {}

    public static boolean initFromEnvironment(@NotNull String serverIdentifier) {
        return init(
                System.getenv("SENTRY_DSN"),
                valueOrDefault(System.getenv("SENTRY_ENVIRONMENT"), "development"),
                System.getenv("SENTRY_RELEASE"),
                serverIdentifier
        );
    }

    public static boolean init(
            @Nullable String dsn,
            @NotNull String environment,
            @Nullable String release,
            @NotNull String serverIdentifier
    ) {
        if (dsn == null || dsn.isBlank()) {
            initialized = false;
            return false;
        }
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment(environment);
            if (release != null && !release.isBlank()) {
                options.setRelease(release);
            }
        });
        Sentry.setTag("server_identifier", serverIdentifier);
        initialized = true;
        return true;
    }

    public static void capture(@NotNull Throwable throwable) {
        if (initialized) {
            Sentry.captureException(throwable);
        }
    }

    public static void capture(@NotNull String message) {
        if (initialized) {
            Sentry.captureMessage(message);
        }
    }

    public static void setServerIdentifier(@NotNull String serverIdentifier) {
        if (initialized) {
            Sentry.setTag("server_identifier", serverIdentifier);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void close() {
        if (initialized) {
            Sentry.close();
            initialized = false;
        }
    }

    @NotNull
    private static String valueOrDefault(@Nullable String value, @NotNull String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

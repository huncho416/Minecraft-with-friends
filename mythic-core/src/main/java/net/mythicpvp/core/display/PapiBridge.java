package net.mythicpvp.core.display;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Optional bridge to PlaceholderAPI. We don't take a hard dependency on
 * PAPI — many small servers don't run it, and the suite must work
 * without it. When PAPI is present at runtime this bridge resolves
 * its tokens via reflection so {@code %papi_*%} style placeholders in
 * tablist/scoreboard templates expand correctly.
 *
 * <p>Why **:** {@link PlaceholderResolver} owns the suite's own tokens
 * (rank, server, online, etc). Anything it doesn't recognise it leaves
 * verbatim, so PAPI gets a clean second pass at unresolved tokens.
 *
 * <p>Threading: PlaceholderAPI's setPlaceholders is documented as main-thread.
 * Callers already invoke us from the display path (main thread) so this is fine.
 */
public final class PapiBridge {

    private static volatile Method setPlaceholders;
    private static volatile boolean checked;

    private PapiBridge() {}

    /**
     * Resolve PAPI tokens against {@code player}, returning the input
     * unchanged when PAPI isn't available or {@code player} is null.
     */
    @NotNull
    public static String apply(@Nullable OfflinePlayer player, @NotNull String text) {
        if (player == null || text.isEmpty() || text.indexOf('%') < 0) {
            return text;
        }
        Method m = resolveMethod();
        if (m == null) {
            return text;
        }
        try {
            Object result = m.invoke(null, player, text);
            return result instanceof String s ? s : text;
        } catch (ReflectiveOperationException e) {
            // One-shot warn so a broken PAPI install doesn't spam logs.
            Logger.getLogger("MythicCore").warning(
                    "[papi] setPlaceholders failed: " + e.getMessage());
            return text;
        }
    }

    /** {@code true} when PlaceholderAPI is loaded and the static method resolved. */
    public static boolean available() {
        return resolveMethod() != null;
    }

    @Nullable
    private static Method resolveMethod() {
        if (checked) {
            return setPlaceholders;
        }
        synchronized (PapiBridge.class) {
            if (checked) {
                return setPlaceholders;
            }
            checked = true;
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                return null;
            }
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                setPlaceholders = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            } catch (ReflectiveOperationException e) {
                Logger.getLogger("MythicCore").warning(
                        "[papi] reflection lookup failed — bridge disabled: " + e.getMessage());
                setPlaceholders = null;
            }
            return setPlaceholders;
        }
    }
}

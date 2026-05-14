package net.mythicpvp.core.display;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public final class PapiBridge {

    private static volatile Method setPlaceholders;
    private static volatile boolean checked;

    private PapiBridge() {}

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

            Logger.getLogger("MythicCore").warning(
                    "[papi] setPlaceholders failed: " + e.getMessage());
            return text;
        }
    }

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

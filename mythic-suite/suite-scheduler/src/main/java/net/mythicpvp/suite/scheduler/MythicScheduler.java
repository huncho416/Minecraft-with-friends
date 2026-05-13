package net.mythicpvp.suite.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class MythicScheduler {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private MythicScheduler() {}

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runSync(@NotNull JavaPlugin plugin, @NotNull Runnable task) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public static void runAsync(@NotNull JavaPlugin plugin, @NotNull Runnable task) {
        if (FOLIA) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runLater(@NotNull JavaPlugin plugin, @NotNull Runnable task, long delayTicks) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runLaterAsync(@NotNull JavaPlugin plugin, @NotNull Runnable task, long delayTicks) {
        if (FOLIA) {
            long delayMs = delayTicks * 50L;
            plugin.getServer().getAsyncScheduler().runDelayed(plugin, t -> task.run(), delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    public static void runTimer(@NotNull JavaPlugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runTimerAsync(@NotNull JavaPlugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            long delayMs = delayTicks * 50L;
            long periodMs = periodTicks * 50L;
            plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), delayMs < 1 ? 1 : delayMs, periodMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runOnEntity(@NotNull JavaPlugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public static void runOnEntityLater(@NotNull JavaPlugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        if (FOLIA) {
            entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runOnEntityTimer(@NotNull JavaPlugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, delayTicks < 1 ? 1 : delayTicks, periodTicks);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runAtLocation(@NotNull JavaPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        if (FOLIA) {
            plugin.getServer().getRegionScheduler().execute(plugin, location, task);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public static void runAtLocationLater(@NotNull JavaPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        if (FOLIA) {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, location, t -> task.run(), delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runAtLocationTimer(@NotNull JavaPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, t -> task.run(), delayTicks < 1 ? 1 : delayTicks, periodTicks);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }
}

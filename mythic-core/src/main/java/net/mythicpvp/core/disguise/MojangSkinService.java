package net.mythicpvp.core.disguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class MojangSkinService {

    private final JavaPlugin plugin;

    public MojangSkinService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Result> lookup(@NotNull String name) {
        CompletableFuture<Result> future = new CompletableFuture<>();
        MythicScheduler.runAsync(plugin, () -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(name);
                profile = profile.update().get();
                if (profile.getId() == null) {
                    future.complete(Result.unknown(name));
                    return;
                }
                String resolvedName = profile.getName() == null ? name : profile.getName();
                String value = null;
                String signature = null;
                for (ProfileProperty property : profile.getProperties()) {
                    if (property.getName().equalsIgnoreCase("textures")) {
                        value = property.getValue();
                        signature = property.getSignature();
                        break;
                    }
                }
                future.complete(Result.success(resolvedName, value, signature));
            } catch (Exception ex) {
                future.complete(Result.failed(name, ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            }
        });
        return future;
    }

    public record Result(@NotNull String requestedName,
                         @Nullable String resolvedName,
                         @Nullable String skinValue,
                         @Nullable String skinSignature,
                         boolean knownAccount,
                         @Nullable String failureReason) {

        public boolean success() {
            return knownAccount && failureReason == null;
        }

        @NotNull
        static Result success(@NotNull String name, @Nullable String value, @Nullable String signature) {
            return new Result(name, name, value, signature, true, null);
        }

        @NotNull
        static Result unknown(@NotNull String name) {
            return new Result(name, null, null, null, false, null);
        }

        @NotNull
        static Result failed(@NotNull String name, @NotNull String reason) {
            return new Result(name, null, null, null, false, reason);
        }
    }
}

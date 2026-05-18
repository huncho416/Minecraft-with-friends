package net.mythicpvp.core.disguise;

import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DisguiseApplier implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, OriginalIdentity> originals = new ConcurrentHashMap<>();
    private volatile Runnable displayRefresh = () -> {};

    public DisguiseApplier(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setDisplayRefresh(@NotNull Runnable refresh) {
        this.displayRefresh = refresh;
    }

    public void apply(@NotNull Player player,
                      @NotNull String displayName,
                      @Nullable String skinValue,
                      @Nullable String skinSignature,
                      @Nullable String rankOverride) {
        UUID uuid = player.getUniqueId();
        originals.computeIfAbsent(uuid, k -> OriginalIdentity.capture(player));

        DisguiseManager.SkinProperties skin = (skinValue != null && skinSignature != null)
                ? new DisguiseManager.SkinProperties(skinValue, skinSignature)
                : null;
        DisguiseManager.getInstance().disguiseAs(uuid, displayName, skin, rankOverride);

        applyProfileAndRefresh(player, displayName, skinValue, skinSignature);
    }

    public void undisguise(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        OriginalIdentity original = originals.remove(uuid);
        DisguiseManager.getInstance().undisguise(uuid);
        if (original != null) {
            applyProfileAndRefresh(player, original.name(), original.skinValue(), original.skinSignature());
        }
    }

    public boolean isDisguised(@NotNull UUID uuid) {
        return DisguiseManager.getInstance().isDisguised(uuid);
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        MythicScheduler.runLater(plugin, () -> {
            for (Player existing : Bukkit.getOnlinePlayers()) {
                if (existing.getUniqueId().equals(joiner.getUniqueId())) {
                    continue;
                }
                if (DisguiseManager.getInstance().isDisguised(existing.getUniqueId())) {
                    rehideShow(joiner, existing);
                }
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        originals.remove(event.getPlayer().getUniqueId());
    }

    private void applyProfileAndRefresh(@NotNull Player player,
                                        @NotNull String displayName,
                                        @Nullable String skinValue,
                                        @Nullable String skinSignature) {
        MythicScheduler.runOnEntity(plugin, player, () -> {
            try {
                PlayerProfile fresh = Bukkit.createProfile(player.getUniqueId(), displayName);
                Set<ProfileProperty> existing = player.getPlayerProfile().getProperties();
                Set<ProfileProperty> next = new java.util.HashSet<>();
                for (ProfileProperty property : existing) {
                    if (!property.getName().equalsIgnoreCase("textures")) {
                        next.add(property);
                    }
                }
                if (skinValue != null) {
                    next.add(new ProfileProperty("textures", skinValue,
                            skinSignature == null ? "" : skinSignature));
                }
                fresh.setProperties(next);
                player.setPlayerProfile(fresh);
                player.displayName(MythicHex.colorize("&#FFFFFF" + displayName));
                player.playerListName(MythicHex.colorize("&#FFFFFF" + displayName));
            } catch (Throwable t) {
                plugin.getLogger().warning("[disguise] failed to apply profile for "
                        + player.getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            try {
                displayRefresh.run();
            } catch (Throwable ignored) {
            }
            MythicScheduler.runLater(plugin, () -> refreshObservers(player), 10L);
        });
    }

    private void refreshObservers(@NotNull Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            rehideShow(viewer, target);
        }
    }

    private void rehideShow(@NotNull Player viewer, @NotNull Player target) {
        MythicScheduler.runSync(plugin, () -> {
            try {
                viewer.hidePlayer(plugin, target);
            } catch (Throwable ignored) {
            }
        });
        MythicScheduler.runLater(plugin, () -> MythicScheduler.runSync(plugin, () -> {
            try {
                viewer.showPlayer(plugin, target);
            } catch (Throwable ignored) {
            }
        }), 20L);
    }

    private record OriginalIdentity(@NotNull String name,
                                    @Nullable String skinValue,
                                    @Nullable String skinSignature) {
        @NotNull
        static OriginalIdentity capture(@NotNull Player player) {
            PlayerProfile profile = player.getPlayerProfile();
            String name = profile.getName() == null ? player.getName() : profile.getName();
            String value = null;
            String signature = null;
            for (ProfileProperty property : profile.getProperties()) {
                if (property.getName().equalsIgnoreCase("textures")) {
                    value = property.getValue();
                    signature = property.getSignature();
                    break;
                }
            }
            return new OriginalIdentity(name, value, signature);
        }
    }
}

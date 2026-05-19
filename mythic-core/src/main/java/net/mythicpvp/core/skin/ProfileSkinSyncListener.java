package net.mythicpvp.core.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.mythicpvp.core.disguise.MojangSkinService;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.packet.PacketAction;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ProfileSkinSyncListener implements Listener {

    private final JavaPlugin plugin;
    private final MojangSkinService skins;

    public ProfileSkinSyncListener(@NotNull JavaPlugin plugin, @NotNull MojangSkinService skins) {
        this.plugin = plugin;
        this.skins = skins;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (DisguiseManager.getInstance().isDisguised(player.getUniqueId())) {
            return;
        }
        if (hasSkin(player) || isBedrockName(player.getName())) {
            return;
        }
        skins.lookup(player.getName()).thenAccept(result -> {
            if (!result.success() || result.skinValue() == null) {
                return;
            }
            MythicScheduler.runOnEntity(plugin, player, () -> applySkin(player, result));
        });
    }

    private void applySkin(@NotNull Player player, @NotNull MojangSkinService.Result result) {
        if (!player.isOnline()) return;
        try {
            PlayerProfile profile = player.getPlayerProfile();
            profile.getProperties().removeIf(prop -> prop.getName().equalsIgnoreCase("textures"));
            profile.getProperties().add(new ProfileProperty(
                    "textures",
                    result.skinValue(),
                    result.skinSignature() == null ? "" : result.skinSignature()));
            player.setPlayerProfile(profile);
        } catch (Throwable t) {
            plugin.getLogger().warning("[skin-sync] setPlayerProfile failed for "
                    + player.getName() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return;
        }
        MythicScheduler.runLater(plugin, () -> refreshObservers(player), 10L);
    }

    private void refreshObservers(@NotNull Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) continue;
            MythicScheduler.runOnEntity(plugin, viewer, () -> {
                try {
                    PacketAction.send(viewer, new PacketAction.EntityRefresh(
                            "skin-sync:" + target.getUniqueId(),
                            target.getUniqueId(),
                            target.getName(),
                            null,
                            null));
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private static boolean hasSkin(@NotNull Player player) {
        try {
            PlayerProfile profile = player.getPlayerProfile();
            for (ProfileProperty property : profile.getProperties()) {
                if (property.getName().equalsIgnoreCase("textures")
                        && property.getValue() != null
                        && !property.getValue().isBlank()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isBedrockName(@NotNull String name) {
        return name.startsWith(".");
    }
}

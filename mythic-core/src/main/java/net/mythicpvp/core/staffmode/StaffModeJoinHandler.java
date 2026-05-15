package net.mythicpvp.core.staffmode;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.staff.StaffChannel;
import net.mythicpvp.core.staff.StaffChannelService;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class StaffModeJoinHandler implements Listener {

    private final JavaPlugin plugin;
    private final StaffModeService staffMode;
    private final StaffStateStore stateStore;
    private final StaffSettings settings;
    private final RankService ranks;
    private final GrantService grants;
    private final StaffChannelService staffChannels;

    public StaffModeJoinHandler(@NotNull JavaPlugin plugin,
                                @NotNull StaffModeService staffMode,
                                @NotNull StaffStateStore stateStore,
                                @NotNull StaffSettings settings,
                                @NotNull RankService ranks,
                                @NotNull GrantService grants,
                                @NotNull StaffChannelService staffChannels) {
        this.plugin = plugin;
        this.staffMode = staffMode;
        this.stateStore = stateStore;
        this.settings = settings;
        this.ranks = ranks;
        this.grants = grants;
        this.staffChannels = staffChannels;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!isStaff(uuid)) {
            return;
        }
        boolean bypass = player.hasPermission(StaffSettings.BYPASS_PERMISSION);

        boolean shouldStaffMode = stateStore.wasInStaffMode(uuid)
                || (!bypass && settings.forceStaffModeOnJoin());
        boolean shouldVanish = stateStore.wasVanished(uuid)
                || (!bypass && settings.forceVanishOnJoin());
        boolean shouldStaffChat = !bypass && settings.forceStaffChatOnJoin();

        if (shouldStaffMode || shouldVanish || shouldStaffChat) {
            MythicScheduler.runLater(plugin, () -> {
                if (!player.isOnline()) return;
                if (shouldStaffMode && !staffMode.inStaffMode(uuid)) {
                    staffMode.enable(player);
                }
                if (shouldVanish && !staffMode.isVanished(uuid)) {
                    staffMode.toggleVanish(player);
                }
                if (shouldStaffChat) {
                    StaffChannel channel = settings.forcedChannel();
                    if (player.hasPermission(channel.permission())
                            && staffChannels.toggledChannel(uuid) != channel) {
                        staffChannels.toggle(uuid, channel);
                    }
                }
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!isStaff(uuid)) {
            return;
        }
        stateStore.recordStaffMode(uuid, staffMode.inStaffMode(uuid));
        stateStore.recordVanish(uuid, staffMode.isVanished(uuid));
    }

    private boolean isStaff(@NotNull UUID uuid) {
        CoreRank rank = ranks.get(grants.activeRank(uuid));
        return rank != null && rank.staff();
    }
}

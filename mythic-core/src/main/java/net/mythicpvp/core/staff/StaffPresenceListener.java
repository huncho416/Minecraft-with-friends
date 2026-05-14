package net.mythicpvp.core.staff;

import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class StaffPresenceListener implements Listener {

    public static final String STAFF_PERMISSION = "mythic.core.staff.notify";

    private final StaffPresenceService presence;
    private final RankService ranks;
    private final GrantService grants;

    public StaffPresenceListener(
            @NotNull StaffPresenceService presence,
            @NotNull RankService ranks,
            @NotNull GrantService grants) {
        this.presence = presence;
        this.ranks = ranks;
        this.grants = grants;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(STAFF_PERMISSION)) {
            return;
        }
        var snapshot = rankSnapshot(player);
        presence.join(player.getUniqueId(), player.getName(), snapshot.name, snapshot.color);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(STAFF_PERMISSION)) {
            return;
        }
        var snapshot = rankSnapshot(player);
        presence.quit(player.getUniqueId(), player.getName(), snapshot.name, snapshot.color);
    }

    @NotNull
    private RankSnapshot rankSnapshot(@NotNull Player player) {
        String rankId = grants.activeRank(player.getUniqueId());
        var rank = ranks.get(rankId);
        if (rank == null) {
            return new RankSnapshot("", "&7");
        }
        return new RankSnapshot(rank.name(), rank.color());
    }

    private record RankSnapshot(@NotNull String name, @NotNull String color) {}
}

package net.mythicpvp.core.staff;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class StaffChatToggleListener implements Listener {

    private final StaffChannelService staffChat;
    private final RankService ranks;
    private final GrantService grants;

    public StaffChatToggleListener(@NotNull StaffChannelService staffChat,
                                   @NotNull RankService ranks,
                                   @NotNull GrantService grants) {
        this.staffChat = staffChat;
        this.ranks = ranks;
        this.grants = grants;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        StaffChannel channel = staffChat.toggledChannel(player.getUniqueId());
        if (channel == null) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        UUID uuid = player.getUniqueId();
        String rankId = grants.activeRank(uuid);
        var rank = ranks.get(rankId);
        staffChat.send(channel, uuid, player.getName(), rank == null ? "" : rank.name(), rank == null ? "&7" : rank.color(), message);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        staffChat.clearToggle(event.getPlayer().getUniqueId());
    }
}

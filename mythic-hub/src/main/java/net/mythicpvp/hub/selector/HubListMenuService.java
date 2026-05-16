package net.mythicpvp.hub.selector;

import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.core.transfer.TransferQueueService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HubListMenuService {

    private static final String HUB_ROLE = "HUB";

    private final ServerSelectorService selectorService;
    private final String localShardId;

    public HubListMenuService(@NotNull ServerSelectorService selectorService, @NotNull String localShardId) {
        this.selectorService = selectorService;
        this.localShardId = localShardId;
    }

    public void open(@NotNull Player player) {
        List<ServerSelectorService.ServerInfo> hubs = selectorService.getServersForRole(HUB_ROLE).stream()
                .filter(s -> !isProxyShard(s.serverId()))
                .sorted(Comparator.comparing(ServerSelectorService.ServerInfo::serverId, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int rows = Math.max(3, (hubs.size() + 8) / 9 + 2);
        MythicMenu menu = MythicMenu.create(rows, "&#F529BEHub Selector");

        if (hubs.isEmpty()) {
            menu.slot(13, MythicItem.create(Material.BARRIER)
                    .name("&#FF8A8ANo hubs available")
                    .lore("&7No hub servers are reachable right now.")
                    .build(), event -> {
            });
            menu.open(player);
            return;
        }

        int slot = startingSlot(hubs.size(), rows);
        for (ServerSelectorService.ServerInfo hub : hubs) {
            boolean current = hub.serverId().equalsIgnoreCase(localShardId);
            menu.slot(slot, buildHubItem(hub, current), event -> {
                if (current) {
                    player.sendMessage(MythicHex.colorize(
                            "&#FFEC8AYou are already connected to &#FFFFFF" + hub.serverId() + "&#FFEC8A."));
                    return;
                }
                player.closeInventory();
                TransferQueueService queue = lookupQueue();
                if (queue != null) {
                    queue.enqueue(player, hub.serverId());
                    return;
                }
                ProxyTransferService transfer = lookupTransfer();
                if (transfer != null) {
                    transfer.transfer(player, hub.serverId());
                } else {
                    player.sendMessage(MythicHex.colorize(
                            "&#FF8A8ATransfer is unavailable on this shard."));
                }
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        menu.open(player);
    }

    private int startingSlot(int count, int rows) {
        if (rows == 3 && count == 1) return 13;
        if (rows == 3 && count <= 7) return 11;
        return 10;
    }

    @NotNull
    private static ItemStack buildHubItem(@NotNull ServerSelectorService.ServerInfo hub, boolean current) {
        Material mat = current ? Material.LIME_CONCRETE : Material.BEACON;
        String header = current ? "&#9CFF9C" : "&#F529BE";
        String statusLine = current ? "&#9CFF9CCurrently connected" : "&#D2D8E0Click to switch";
        String tps = String.format(Locale.ROOT, "%.1f", hub.tps());
        return MythicItem.create(mat)
                .name(header + hub.serverId())
                .lore(
                        "&7Players: &f" + hub.playerCount(),
                        "&7TPS: &f" + tps,
                        "&7Status: " + (current ? "&aOnline (you)" : "&aOnline"),
                        "",
                        statusLine)
                .build();
    }

    private static boolean isProxyShard(@NotNull String shardId) {
        String lower = shardId.toLowerCase(Locale.ROOT);
        return lower.startsWith("proxy-") || lower.equals("proxy");
    }

    @Nullable
    private static TransferQueueService lookupQueue() {
        RegisteredServiceProvider<TransferQueueService> rsp =
                Bukkit.getServicesManager().getRegistration(TransferQueueService.class);
        return rsp == null ? null : rsp.getProvider();
    }

    @Nullable
    private static ProxyTransferService lookupTransfer() {
        RegisteredServiceProvider<ProxyTransferService> rsp =
                Bukkit.getServicesManager().getRegistration(ProxyTransferService.class);
        return rsp == null ? null : rsp.getProvider();
    }
}

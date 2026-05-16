package net.mythicpvp.hub.selector;

import net.mythicpvp.core.transfer.ProxyTransferService;
import net.mythicpvp.core.transfer.TransferQueueService;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ServerSelectorMenu {

    private static final String HUB_ROLE = "HUB";

    private final ServerSelectorService selectorService;
    private final Random random = new Random();

    public ServerSelectorMenu(@NotNull ServerSelectorService selectorService) {
        this.selectorService = selectorService;
    }

    public void openGroupMenu(@NotNull Player player) {
        MythicMenu menu = MythicMenu.create(3, "&#F529BEServer Selector");

        List<ServerSelectorService.ServerGroup> groups = visibleGroups();
        int[] slots = centeredSlots(groups.size());

        for (int i = 0; i < groups.size() && i < slots.length; i++) {
            ServerSelectorService.ServerGroup group = groups.get(i);
            List<ServerSelectorService.ServerInfo> servers = selectorService.getServersForRole(group.role());
            int totalPlayers = servers.stream().mapToInt(ServerSelectorService.ServerInfo::playerCount).sum();

            menu.slot(slots[i], buildGroupItem(group, servers.size(), totalPlayers),
                    event -> joinRandomShard(player, group));
        }

        menu.open(player);
    }

    @NotNull
    private static int[] centeredSlots(int count) {
        // 3-row chest, content row = 9..17, center = 13
        return switch (count) {
            case 0, 1 -> new int[]{13};
            case 2 -> new int[]{12, 14};
            case 3 -> new int[]{12, 13, 14};
            case 4 -> new int[]{11, 12, 14, 15};
            case 5 -> new int[]{11, 12, 13, 14, 15};
            case 6 -> new int[]{10, 11, 12, 14, 15, 16};
            default -> new int[]{10, 11, 12, 13, 14, 15, 16};
        };
    }

    @NotNull
    private static org.bukkit.inventory.ItemStack buildGroupItem(
            @NotNull ServerSelectorService.ServerGroup group,
            int serverCount,
            int totalPlayers) {
        String status = serverCount > 0 ? "&#9CFF9COnline" : "&#FF8A8AOffline";
        java.util.Map<String, String> placeholders = java.util.Map.of(
                "release_date", group.releaseDate(),
                "age", group.age(),
                "servers", Integer.toString(serverCount),
                "players", Integer.toString(totalPlayers),
                "status", status);
        java.util.List<String> lore = new java.util.ArrayList<>();
        if (!group.tagline().isEmpty()) {
            lore.add(group.tagline());
            lore.add("");
        }
        if (group.lore().isEmpty()) {
            lore.add("&7Status: " + status);
            lore.add("&7Players: &f" + totalPlayers);
            lore.add("");
            lore.add("&#9CFF9CClick to join");
        } else {
            for (String line : group.lore()) {
                String resolved = line;
                for (var e : placeholders.entrySet()) {
                    resolved = resolved.replace("%" + e.getKey() + "%", e.getValue());
                }
                lore.add(resolved);
            }
        }
        return MythicItem.create(group.material())
                .name(group.displayName())
                .lore(lore)
                .build();
    }

    private void joinRandomShard(@NotNull Player player, @NotNull ServerSelectorService.ServerGroup group) {
        List<ServerSelectorService.ServerInfo> servers = selectorService.getServersForRole(group.role());
        if (servers.isEmpty()) {
            player.closeInventory();
            player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(
                    "&#FF8A8ANo " + group.displayName() + " &#FF8A8Aservers are available right now."));
            return;
        }
        ServerSelectorService.ServerInfo target = servers.get(random.nextInt(servers.size()));
        player.closeInventory();
        TransferQueueService queue = lookupQueue();
        if (queue != null) {
            queue.enqueue(player, target.serverId());
            return;
        }
        ProxyTransferService transfer = lookupTransfer();
        if (transfer != null) {
            transfer.transfer(player, target.serverId());
        } else {
            player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(
                    "&#FF8A8ATransfer is unavailable on this shard."));
        }
    }

    @Nullable
    private TransferQueueService lookupQueue() {
        RegisteredServiceProvider<TransferQueueService> rsp =
                Bukkit.getServicesManager().getRegistration(TransferQueueService.class);
        return rsp == null ? null : rsp.getProvider();
    }

    @Nullable
    private ProxyTransferService lookupTransfer() {
        RegisteredServiceProvider<ProxyTransferService> rsp =
                Bukkit.getServicesManager().getRegistration(ProxyTransferService.class);
        return rsp == null ? null : rsp.getProvider();
    }

    @NotNull
    private List<ServerSelectorService.ServerGroup> visibleGroups() {
        List<ServerSelectorService.ServerGroup> filtered = new ArrayList<>();
        for (ServerSelectorService.ServerGroup group : selectorService.getGroups()) {
            if (HUB_ROLE.equalsIgnoreCase(group.role())) {
                continue;
            }
            filtered.add(group);
        }
        return filtered;
    }
}

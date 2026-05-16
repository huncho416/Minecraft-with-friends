package net.mythicpvp.hub.selector;

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
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int count = Math.min(groups.size(), slots.length);

        for (int i = 0; i < count; i++) {
            ServerSelectorService.ServerGroup group = groups.get(i);
            List<ServerSelectorService.ServerInfo> servers = selectorService.getServersForRole(group.role());
            int totalPlayers = servers.stream().mapToInt(ServerSelectorService.ServerInfo::playerCount).sum();

            menu.slot(slots[i], MythicItem.create(group.material())
                    .name(group.displayName())
                    .lore(List.of(
                            "&7Servers: &f" + servers.size(),
                            "&7Players: &f" + totalPlayers,
                            "&#D2D8E0Click to join"))
                    .build(), event -> joinRandomShard(player, group));
        }

        menu.open(player);
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
        } else {
            player.transfer(target.serverId(), 25565);
        }
    }

    @Nullable
    private TransferQueueService lookupQueue() {
        RegisteredServiceProvider<TransferQueueService> rsp =
                Bukkit.getServicesManager().getRegistration(TransferQueueService.class);
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

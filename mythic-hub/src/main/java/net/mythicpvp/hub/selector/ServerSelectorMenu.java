package net.mythicpvp.hub.selector;

import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ServerSelectorMenu {

    private final ServerSelectorService selectorService;

    public ServerSelectorMenu(@NotNull ServerSelectorService selectorService) {
        this.selectorService = selectorService;
    }

    public void openGroupMenu(@NotNull Player player) {
        MythicMenu menu = MythicMenu.create(3, "&#F529BEServer Selector");

        List<ServerSelectorService.ServerGroup> groups = selectorService.getGroups();
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
                            "&#D2D8E0Click to browse"))
                    .build(), event -> openServerList(player, group));
        }

        menu.open(player);
    }

    public void openServerList(@NotNull Player player, @NotNull ServerSelectorService.ServerGroup group) {
        PaginatedMenu menu = PaginatedMenu.create(3, "&#F529BE" + group.displayName());

        List<ServerSelectorService.ServerInfo> servers = selectorService.getServersForRole(group.role());
        for (ServerSelectorService.ServerInfo server : servers) {
            Material statusMat = server.tps() >= 18.0 ? Material.LIME_DYE : Material.ORANGE_DYE;
            menu.addItem(MythicItem.create(statusMat)
                    .name("&#F529BE" + server.serverId())
                    .lore(List.of(
                            "&7Players: &f" + server.playerCount(),
                            "&7TPS: &f" + String.format("%.1f", server.tps()),
                            "&#D2D8E0Click to connect"))
                    .build(), event -> {
                player.closeInventory();
                player.transfer(server.serverId(), 25565);
            });
        }

        menu.open(player);
    }
}

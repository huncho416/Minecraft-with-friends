package net.mythicpvp.hub.selector;

import net.mythicpvp.suite.config.MythicConfig;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerSelectorService {

    private final List<ServerGroup> groups = new ArrayList<>();
    private final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();

    public void loadGroups(@NotNull MythicConfig config) {
        groups.clear();
        List<Map<?, ?>> groupList = config.getConfig().getMapList("selector.groups");

        for (Map<?, ?> map : groupList) {
            Object roleObj = map.get("role");
            String role = roleObj != null ? String.valueOf(roleObj) : "";
            Object nameObj = map.get("display-name");
            String displayName = nameObj != null ? String.valueOf(nameObj) : role;
            Material material;
            try {
                Object matObj = map.get("material");
                material = Material.valueOf(matObj != null ? String.valueOf(matObj) : "CHEST");
            } catch (IllegalArgumentException e) {
                material = Material.CHEST;
            }
            groups.add(new ServerGroup(role, displayName, material));
        }
    }

    public void updateServer(@NotNull String serverId, @NotNull String role, int playerCount, double tps, boolean healthy) {
        servers.put(serverId, new ServerInfo(serverId, role, playerCount, tps, healthy));
    }

    public void removeServer(@NotNull String serverId) {
        servers.remove(serverId);
    }

    @NotNull
    public List<ServerGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    @NotNull
    public List<ServerInfo> getServersForRole(@NotNull String role) {
        return servers.values().stream()
                .filter(s -> s.role().equalsIgnoreCase(role))
                .filter(ServerInfo::healthy)
                .toList();
    }

    @NotNull
    public List<ServerInfo> getAllHealthyServers() {
        return servers.values().stream()
                .filter(ServerInfo::healthy)
                .toList();
    }

    public record ServerGroup(@NotNull String role, @NotNull String displayName, @NotNull Material material) {}

    public record ServerInfo(@NotNull String serverId, @NotNull String role, int playerCount, double tps, boolean healthy) {}
}

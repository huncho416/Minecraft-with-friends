package net.mythicpvp.suite.nametag;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NametagManager {

    private static final NametagManager INSTANCE = new NametagManager();
    private final Map<UUID, NametagData> nametags = new ConcurrentHashMap<>();

    private NametagManager() {}

    @NotNull
    public static NametagManager getInstance() {
        return INSTANCE;
    }

    public void setNametag(@NotNull Player player, @NotNull String prefix, @NotNull String suffix, int sortWeight) {
        nametags.put(player.getUniqueId(), new NametagData(prefix, suffix, sortWeight));
        applyToAll(player);
    }

    public void setPrefix(@NotNull Player player, @NotNull String prefix) {
        NametagData data = nametags.getOrDefault(player.getUniqueId(), NametagData.EMPTY);
        nametags.put(player.getUniqueId(), new NametagData(prefix, data.suffix(), data.sortWeight()));
        applyToAll(player);
    }

    public void setSuffix(@NotNull Player player, @NotNull String suffix) {
        NametagData data = nametags.getOrDefault(player.getUniqueId(), NametagData.EMPTY);
        nametags.put(player.getUniqueId(), new NametagData(data.prefix(), suffix, data.sortWeight()));
        applyToAll(player);
    }

    private void applyToAll(@NotNull Player target) {
        NametagData data = nametags.get(target.getUniqueId());
        if (data == null) return;

        for (Player viewer : target.getServer().getOnlinePlayers()) {
            applyFor(viewer, target, data);
        }
    }

    public void applyFor(@NotNull Player viewer, @NotNull Player target) {
        NametagData data = nametags.get(target.getUniqueId());
        if (data == null) return;
        applyFor(viewer, target, data);
    }

    private void applyFor(@NotNull Player viewer, @NotNull Player target, @NotNull NametagData data) {
        Scoreboard board = viewer.getScoreboard();
        String teamName = String.format("%03d_%s", data.sortWeight(), target.getName());
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        team.prefix(MythicHex.colorize(data.prefix()));
        team.suffix(MythicHex.colorize(data.suffix()));

        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    public void remove(@NotNull Player player) {
        nametags.remove(player.getUniqueId());
        for (Player viewer : player.getServer().getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            for (Team team : board.getTeams()) {
                if (team.hasEntry(player.getName())) {
                    team.removeEntry(player.getName());
                    if (team.getEntries().isEmpty()) {
                        team.unregister();
                    }
                    break;
                }
            }
        }
    }

    @Nullable
    public NametagData getNametag(@NotNull Player player) {
        return nametags.get(player.getUniqueId());
    }

    public record NametagData(@NotNull String prefix, @NotNull String suffix, int sortWeight) {
        public static final NametagData EMPTY = new NametagData("", "", 999);
    }
}

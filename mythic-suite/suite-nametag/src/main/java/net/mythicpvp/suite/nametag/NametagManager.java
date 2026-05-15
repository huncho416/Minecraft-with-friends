package net.mythicpvp.suite.nametag;

import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.packet.PacketAction;
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
    private static final boolean FOLIA = detectFolia();
    private final Map<UUID, NametagData> nametags = new ConcurrentHashMap<>();

    private NametagManager() {}

    @NotNull
    public static NametagManager getInstance() {
        return INSTANCE;
    }

    public void setNametag(@NotNull Player player, @NotNull String prefix, @NotNull String suffix, int sortWeight) {
        setNametag(player, prefix, suffix, sortWeight, null);
    }

    public void setNametag(@NotNull Player player, @NotNull String prefix, @NotNull String suffix, int sortWeight, @Nullable String glowColor) {
        nametags.put(player.getUniqueId(), new NametagData(prefix, suffix, sortWeight, glowColor));
        applyToAll(player);
    }

    public void loadNametag(@NotNull Player player, @NotNull ConfigText text, @NotNull String key) {
        setNametag(
                player,
                text.raw(key + ".prefix", ""),
                text.raw(key + ".suffix", ""),
                Integer.parseInt(text.raw(key + ".sort-weight", "999")),
                text.raw(key + ".glow-color", "")
        );
    }

    public void setPrefix(@NotNull Player player, @NotNull String prefix) {
        NametagData data = nametags.getOrDefault(player.getUniqueId(), NametagData.EMPTY);
        nametags.put(player.getUniqueId(), new NametagData(prefix, data.suffix(), data.sortWeight(), data.glowColor()));
        applyToAll(player);
    }

    public void setSuffix(@NotNull Player player, @NotNull String suffix) {
        NametagData data = nametags.getOrDefault(player.getUniqueId(), NametagData.EMPTY);
        nametags.put(player.getUniqueId(), new NametagData(data.prefix(), suffix, data.sortWeight(), data.glowColor()));
        applyToAll(player);
    }

    public void setGlowColor(@NotNull Player player, @Nullable String glowColor) {
        NametagData data = nametags.getOrDefault(player.getUniqueId(), NametagData.EMPTY);
        nametags.put(player.getUniqueId(), new NametagData(data.prefix(), data.suffix(), data.sortWeight(), glowColor));
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
        if (!FOLIA) {
            Scoreboard board = viewer.getScoreboard();
            String teamName = teamName(data.sortWeight(), target.getUniqueId());
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
        String displayName = DisguiseManager.getInstance().getVisibleName(viewer.getUniqueId(), target.getUniqueId(), target.getName());
        PacketAction.send(viewer, new PacketAction.NametagState(
                "nametag:" + target.getUniqueId(),
                target.getUniqueId(),
                MythicHex.colorize(data.prefix()),
                MythicHex.colorize(data.suffix()),
                data.sortWeight(),
                data.glowColor(),
                displayName
        ));
    }

    @NotNull
    private String teamName(int sortWeight, @NotNull UUID uuid) {
        return ("%03d_%s".formatted(sortWeight, uuid.toString().replace("-", ""))).substring(0, 16);
    }

    public void remove(@NotNull Player player) {
        nametags.remove(player.getUniqueId());
        if (FOLIA) {
            return;
        }
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

    public void clear() {
        nametags.clear();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public record NametagData(@NotNull String prefix, @NotNull String suffix, int sortWeight, @Nullable String glowColor) {
        public static final NametagData EMPTY = new NametagData("", "", 999, null);
    }
}

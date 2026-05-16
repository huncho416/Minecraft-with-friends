package net.mythicpvp.core.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MotdListener implements Listener {

    private final MotdService service;

    public MotdListener(@NotNull MotdService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPing(@NotNull PaperServerListPingEvent event) {
        String line1 = service.pickLine1();
        String line2 = service.pickLine2();
        Component motd = MythicHex.colorize(line1).appendNewline().append(MythicHex.colorize(line2));
        event.motd(motd);
        if (service.hidePlayerCount()) {
            event.setHidePlayers(true);
        } else if (service.maxPlayersOverride() > 0) {
            event.setMaxPlayers(service.maxPlayersOverride());
        }
    }

    public static final class MotdService {
        private List<String> line1Pool = List.of();
        private List<String> line2Pool = List.of();
        private boolean hidePlayerCount;
        private int maxPlayersOverride;

        public void load(@NotNull MythicConfig config) {
            this.line1Pool = config.getStringList("motd.line1");
            if (line1Pool.isEmpty()) {
                line1Pool = List.of("&#F529BE&lMythic&#FFFFFFPvP &#D2D8E0&onetwork");
            }
            this.line2Pool = config.getStringList("motd.line2");
            if (line2Pool.isEmpty()) {
                line2Pool = List.of("&#9CC3FFplay.mythicpvp.net &7&l| &#FFEC8AJoin our Discord at &#9CC3FFdiscord.gg/mythicpvp");
            }
            this.hidePlayerCount = config.getBoolean("motd.hide-player-count", false);
            this.maxPlayersOverride = config.getInt("motd.max-players-override", 0);
        }

        @NotNull
        public String pickLine1() {
            return pick(line1Pool);
        }

        @NotNull
        public String pickLine2() {
            return pick(line2Pool);
        }

        public boolean hidePlayerCount() {
            return hidePlayerCount;
        }

        public int maxPlayersOverride() {
            return maxPlayersOverride;
        }

        @NotNull
        private static String pick(@NotNull List<String> pool) {
            if (pool.isEmpty()) return "";
            if (pool.size() == 1) return pool.get(0);
            return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
    }
}

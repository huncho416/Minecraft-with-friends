package net.mythicpvp.core.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

public final class MotdListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final MotdService service;
    private final IntSupplier networkOnlineCounter;

    public MotdListener(@NotNull MotdService service, @NotNull IntSupplier networkOnlineCounter) {
        this.service = service;
        this.networkOnlineCounter = networkOnlineCounter;
    }

    public MotdListener(@NotNull MotdService service) {
        this(service, () -> Bukkit.getOnlinePlayers().size());
    }

    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("removal")
    public void onPing(@NotNull PaperServerListPingEvent event) {
        String line1 = service.pickLine1();
        String line2 = service.pickLine2();
        Component motd = MythicHex.colorize(line1).appendNewline().append(MythicHex.colorize(line2));
        event.motd(motd);

        int networkOnline = Math.max(0, networkOnlineCounter.getAsInt());
        if (service.hidePlayerCount()) {
            event.setHidePlayers(true);
        } else {
            event.setNumPlayers(networkOnline);
            if (service.maxPlayersOverride() > 0) {
                event.setMaxPlayers(service.maxPlayersOverride());
            }
        }

        if (!service.hidePlayerCount() && !service.hoverLines().isEmpty()) {
            try {
                List<PlayerProfile> sample = event.getPlayerSample();
                sample.clear();
                for (String hoverLine : service.hoverLines()) {
                    String resolved = hoverLine
                            .replace("%online%", Integer.toString(networkOnline))
                            .replace("%max%", Integer.toString(event.getMaxPlayers()));
                    String legacy = LEGACY.serialize(MythicHex.colorize(resolved));
                    PlayerProfile profile = createUncheckedProfile(stableUuidFor(legacy), legacy);
                    if (profile != null) {
                        sample.add(profile);
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    @NotNull
    private static UUID stableUuidFor(@NotNull String key) {
        return UUID.nameUUIDFromBytes(("mythic-motd:" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Nullable
    private static PlayerProfile createUncheckedProfile(@NotNull UUID uuid, @NotNull String name) {
        try {
            Class<?> gameProfileCls = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileCls.getConstructor(UUID.class, String.class)
                    .newInstance(uuid, name);
            Class<?> craftCls = Class.forName("com.destroystokyo.paper.profile.CraftPlayerProfile");
            java.lang.reflect.Constructor<?> ctor = craftCls.getDeclaredConstructor(gameProfileCls);
            ctor.setAccessible(true);
            return (PlayerProfile) ctor.newInstance(gameProfile);
        } catch (Throwable t) {
            return null;
        }
    }

    public static final class MotdService {
        private List<String> line1Pool = List.of();
        private List<String> line2Pool = List.of();
        private List<String> hoverLines = List.of();
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
            this.hoverLines = config.getStringList("motd.hover");
            if (hoverLines.isEmpty()) {
                hoverLines = defaultHoverLines();
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

        @NotNull
        public List<String> hoverLines() {
            return hoverLines;
        }

        public boolean hidePlayerCount() {
            return hidePlayerCount;
        }

        public int maxPlayersOverride() {
            return maxPlayersOverride;
        }

        @NotNull
        private static List<String> defaultHoverLines() {
            List<String> defaults = new ArrayList<>();
            defaults.add("&#F529BE&lMYTHIC&#FFFFFF&lPVP");
            defaults.add("&#9CC3FFSTORE &8» &#FFFFFFstore.mythicpvp.net");
            defaults.add("&#9CC3FFDISCORD &8» &#FFFFFFdiscord.gg/mythicpvp");
            defaults.add("&#9CC3FFWEBSITE &8» &#FFFFFFwww.mythicpvp.net");
            defaults.add("&7Online: &#9CFF9C%online%");
            return defaults;
        }

        @NotNull
        private static String pick(@NotNull List<String> pool) {
            if (pool.isEmpty()) return "";
            if (pool.size() == 1) return pool.get(0);
            return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
    }
}

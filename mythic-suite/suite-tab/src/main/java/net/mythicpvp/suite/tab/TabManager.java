package net.mythicpvp.suite.tab;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TabManager {

    private static final TabManager INSTANCE = new TabManager();
    private final Map<UUID, TabLayout> layouts = new ConcurrentHashMap<>();
    private String defaultHeader = "";
    private String defaultFooter = "";

    private TabManager() {}

    @NotNull
    public static TabManager getInstance() {
        return INSTANCE;
    }

    public void setDefaults(@NotNull String header, @NotNull String footer) {
        this.defaultHeader = header;
        this.defaultFooter = footer;
    }

    public void apply(@NotNull Player player) {
        TabLayout layout = layouts.getOrDefault(player.getUniqueId(), new TabLayout(defaultHeader, defaultFooter));
        player.sendPlayerListHeaderAndFooter(
                MythicHex.colorize(layout.getHeader()),
                MythicHex.colorize(layout.getFooter())
        );
    }

    public void setLayout(@NotNull Player player, @NotNull String header, @NotNull String footer) {
        layouts.put(player.getUniqueId(), new TabLayout(header, footer));
        apply(player);
    }

    public void setHeader(@NotNull Player player, @NotNull String header) {
        TabLayout existing = layouts.getOrDefault(player.getUniqueId(), new TabLayout(defaultHeader, defaultFooter));
        layouts.put(player.getUniqueId(), new TabLayout(header, existing.getFooter()));
        apply(player);
    }

    public void setFooter(@NotNull Player player, @NotNull String footer) {
        TabLayout existing = layouts.getOrDefault(player.getUniqueId(), new TabLayout(defaultHeader, defaultFooter));
        layouts.put(player.getUniqueId(), new TabLayout(existing.getHeader(), footer));
        apply(player);
    }

    public void remove(@NotNull Player player) {
        layouts.remove(player.getUniqueId());
    }

    public void applyAll() {
        org.bukkit.Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    public static class TabLayout {
        private final String header;
        private final String footer;

        public TabLayout(@NotNull String header, @NotNull String footer) {
            this.header = header;
            this.footer = footer;
        }

        @NotNull public String getHeader() { return header; }
        @NotNull public String getFooter() { return footer; }
    }
}

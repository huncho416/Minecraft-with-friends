package net.mythicpvp.suite.tab;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.packet.PacketAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public final class TabManager {

    private static final TabManager INSTANCE = new TabManager();
    private final Map<UUID, TabLayout> layouts = new ConcurrentHashMap<>();
    private final Map<UUID, TabEntry> entries = new ConcurrentHashMap<>();
    private volatile String defaultHeader = "";
    private volatile String defaultFooter = "";
    private volatile String fontKey = "";
    private volatile BiPredicate<Player, Player> visibilityPredicate = (viewer, target) -> true;

    private TabManager() {}

    @NotNull
    public static TabManager getInstance() {
        return INSTANCE;
    }

    public void setDefaults(@NotNull String header, @NotNull String footer) {
        this.defaultHeader = header;
        this.defaultFooter = footer;
    }

    public void loadDefaults(@NotNull ConfigText text) {
        setDefaults(text.raw("tab.header", defaultHeader), text.raw("tab.footer", defaultFooter));
        setFontKey(text.raw("tab.font", fontKey));
    }

    public void setFontKey(@NotNull String fontKey) {
        this.fontKey = fontKey;
    }

    @NotNull
    public String getFontKey() {
        return fontKey;
    }

    public void setEntry(@NotNull UUID player, @NotNull String prefix, @NotNull String suffix, int sortWeight) {
        entries.put(player, new TabEntry(prefix, suffix, sortWeight));
    }

    public void setVisibilityPredicate(@NotNull BiPredicate<Player, Player> visibilityPredicate) {
        this.visibilityPredicate = visibilityPredicate;
    }

    public void apply(@NotNull Player player) {
        TabLayout layout = layouts.getOrDefault(player.getUniqueId(), new TabLayout(defaultHeader, defaultFooter));
        Component header = MythicHex.font(fontKey, layout.header());
        Component footer = MythicHex.font(fontKey, layout.footer());
        player.sendPlayerListHeaderAndFooter(header, footer);
        PacketAction.send(player, new PacketAction.TabHeaderFooter("tab:" + player.getUniqueId(), header, footer, visibleEntries(player)));
    }

    @NotNull
    public Map<UUID, Component> visibleEntries(@NotNull Player viewer) {
        Map<UUID, Component> sorted = new LinkedHashMap<>();
        Bukkit.getOnlinePlayers().stream()
                .filter(target -> visibilityPredicate.test(viewer, target))
                .sorted(Comparator.comparingInt(player -> entries.getOrDefault(player.getUniqueId(), TabEntry.EMPTY).sortWeight()))
                .forEach(target -> {
                    TabEntry entry = entries.getOrDefault(target.getUniqueId(), TabEntry.EMPTY);
                    String name = DisguiseManager.getInstance().getVisibleName(viewer.getUniqueId(), target.getUniqueId(), target.getName());
                    sorted.put(target.getUniqueId(), MythicHex.font(fontKey, entry.prefix() + name + entry.suffix()));
                });
        return sorted;
    }

    public void setLayout(@NotNull Player player, @NotNull String header, @NotNull String footer) {
        layouts.put(player.getUniqueId(), new TabLayout(header, footer));
        apply(player);
    }

    public void setHeader(@NotNull Player player, @NotNull String header) {
        TabLayout existing = layouts.getOrDefault(player.getUniqueId(), new TabLayout(defaultHeader, defaultFooter));
        layouts.put(player.getUniqueId(), new TabLayout(header, existing.footer()));
        apply(player);
    }

    public void setFooter(@NotNull Player player, @NotNull String footer) {
        TabLayout existing = layouts.getOrDefault(player.getUniqueId(), new TabLayout(defaultHeader, defaultFooter));
        layouts.put(player.getUniqueId(), new TabLayout(existing.header(), footer));
        apply(player);
    }

    public void remove(@NotNull Player player) {
        layouts.remove(player.getUniqueId());
        entries.remove(player.getUniqueId());
    }

    public void applyAll() {
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    public void clear() {
        layouts.clear();
        entries.clear();
        defaultHeader = "";
        defaultFooter = "";
        fontKey = "";
        visibilityPredicate = (viewer, target) -> true;
    }

    public record TabLayout(@NotNull String header, @NotNull String footer) {}

    public record TabEntry(@NotNull String prefix, @NotNull String suffix, int sortWeight) {
        public static final TabEntry EMPTY = new TabEntry("", "", 999);
    }
}

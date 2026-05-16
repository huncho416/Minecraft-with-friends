package net.mythicpvp.core.staff;

import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.session.SessionTracker;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StaffListMenuService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final RankService rankService;
    private final GrantService grantService;
    private final SessionTracker sessionTracker;

    public StaffListMenuService(@NotNull RankService rankService,
                                @NotNull GrantService grantService,
                                @NotNull SessionTracker sessionTracker) {
        this.rankService = rankService;
        this.grantService = grantService;
        this.sessionTracker = sessionTracker;
    }

    public void open(@NotNull Player viewer) {
        PaginatedMenu menu = PaginatedMenu.create(3, "&#F529BEOnline Staff");
        List<StaffEntry> staff = collectStaff();
        if (staff.isEmpty()) {
            viewer.sendMessage(MythicHex.colorize("&#FFEC8ANo staff are online right now."));
            return;
        }
        for (StaffEntry entry : staff) {
            menu.addItem(buildHead(entry), event -> {
                Player target = Bukkit.getPlayer(entry.uuid);
                if (target == null || !target.isOnline()) {
                    viewer.sendMessage(MythicHex.colorize(
                            "&#FF8A8A" + entry.name + " &#FF8A8Ais no longer online."));
                    open(viewer);
                    return;
                }
                if (event.getClick().isLeftClick()) {
                    viewer.closeInventory();
                    viewer.teleportAsync(target.getLocation());
                    viewer.sendMessage(MythicHex.colorize(
                            "&#9CFF9CTeleported to &#FFFFFF" + entry.name + "&#9CFF9C."));
                }
            });
        }
        menu.open(viewer, 0);
    }

    @NotNull
    private ItemStack buildHead(@NotNull StaffEntry entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid));
            String prefix = entry.rank == null ? "&7" : ampHex(entry.rank.color());
            meta.displayName(MythicHex.colorize(prefix + entry.name));
            String rankName = entry.rank == null ? "Default" : entry.rank.name();
            String rankColor = entry.rank == null ? "&7" : ampHex(entry.rank.color());
            String loggedAt = TIME_FORMAT.format(Instant.ofEpochMilli(entry.loginMillis));
            String session = formatDuration(System.currentTimeMillis() - entry.loginMillis);
            meta.lore(List.of(
                    MythicHex.colorize("&7Rank: " + rankColor + rankName),
                    MythicHex.colorize("&7Logged in: &f" + loggedAt + " &8(" + session + " ago)"),
                    MythicHex.colorize(""),
                    MythicHex.colorize("&#9CFF9CLeft-click to teleport.")));
            head.setItemMeta(meta);
        }
        return head;
    }

    @NotNull
    private List<StaffEntry> collectStaff() {
        List<StaffEntry> entries = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            CoreRank rank = rankService.get(grantService.activeRank(online.getUniqueId()));
            if (rank == null || !rank.staff()) continue;
            entries.add(new StaffEntry(online.getUniqueId(), online.getName(), rank,
                    sessionTracker.loginTime(online)));
        }
        entries.sort(Comparator
                .comparingInt((StaffEntry e) -> e.rank == null ? Integer.MAX_VALUE : e.rank.weight())
                .thenComparing(e -> e.name.toLowerCase()));
        return entries;
    }

    @NotNull
    private static String ampHex(@NotNull String color) {
        if (color.startsWith("#") && !color.startsWith("&#")) {
            return "&" + color;
        }
        return color;
    }

    @NotNull
    private static String formatDuration(long millis) {
        Duration d = Duration.ofMillis(Math.max(0, millis));
        long h = d.toHours();
        long m = d.toMinutesPart();
        if (h > 0) return h + "h " + m + "m";
        long s = d.toSecondsPart();
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private static final class StaffEntry {
        final java.util.UUID uuid;
        final String name;
        final CoreRank rank;
        final long loginMillis;

        StaffEntry(@NotNull java.util.UUID uuid, @NotNull String name, CoreRank rank, long loginMillis) {
            this.uuid = uuid;
            this.name = name;
            this.rank = rank;
            this.loginMillis = loginMillis;
        }
    }
}

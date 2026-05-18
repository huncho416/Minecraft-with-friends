package net.mythicpvp.core.report;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ReportMenuService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public static final String REMOVE_RESOLVED_PERMISSION = "mythic.core.report.admin.remove";

    private final ReportService reportService;
    private final ChatPromptService prompts;
    private final String localShardId;
    private net.mythicpvp.core.session.CrossShardPresenceService presence;
    private net.mythicpvp.core.transfer.ShardRegistry shardRegistry;

    public ReportMenuService(@NotNull ReportService reportService,
                             @NotNull ChatPromptService prompts,
                             @NotNull String localShardId) {
        this.reportService = reportService;
        this.prompts = prompts;
        this.localShardId = localShardId;
    }

    public void setPresence(@NotNull net.mythicpvp.core.session.CrossShardPresenceService presence) {
        this.presence = presence;
    }

    public void setShardRegistry(@NotNull net.mythicpvp.core.transfer.ShardRegistry registry) {
        this.shardRegistry = registry;
    }

    public void openCategoryPicker(@NotNull Player reporter,
                                   @NotNull String targetName,
                                   @NotNull UUID targetUuid,
                                   @NotNull BiConsumer<ReportCategory, String> onSubmit) {
        List<ReportCategory> categories = ReportCategory.all();
        int rows = Math.max(3, (categories.size() + 8) / 9 + 2);
        MythicMenu menu = MythicMenu.create(rows, "&#F529BEReport &#FFFFFF" + targetName);

        int slot = 10;
        for (ReportCategory category : categories) {
            menu.slot(slot, MythicItem.create(category.icon())
                    .name("&#FF8A8A" + category.displayName())
                    .lore(
                            "&7" + category.description(),
                            "",
                            "&#9CFF9CClick to submit this report.")
                    .build(), event -> {
                event.getWhoClicked().closeInventory();
                onSubmit.accept(category, targetName);
            });
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        menu.open(reporter);
    }

    public void openOverview(@NotNull Player staff) {
        MythicMenu menu = MythicMenu.create(3, "&#F529BEReports");
        menu.slot(11, MythicItem.create(Material.WRITABLE_BOOK)
                .name("&#FF8A8AActive Reports")
                .lore(
                        "&7Open reports awaiting staff review.",
                        "&7Count: &f" + reportService.active().size(),
                        "",
                        "&#9CFF9CClick to view.")
                .build(), event -> openActive(staff, 0));
        menu.slot(15, MythicItem.create(Material.BOOK)
                .name("&#9CFF9CResolved Reports")
                .lore(
                        "&7Reports staff have completed.",
                        "&7Count: &f" + reportService.resolved().size(),
                        "",
                        "&#9CFF9CClick to view.")
                .build(), event -> openResolved(staff, 0));
        menu.open(staff);
    }

    public void openActive(@NotNull Player staff, int page) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#F529BEActive Reports");
        for (Report report : reportService.active()) {
            menu.addItem(buildActiveReportItem(report), event -> {
                if (event.getClick().isRightClick()) {
                    promptResolution(staff, report);
                }
            });
        }
        menu.staticSlot(49, MythicItem.create(Material.BARRIER).name("&#FF8A8ABack").build(),
                event -> openOverview(staff));
        menu.open(staff, page);
    }

    public void openResolved(@NotNull Player staff, int page) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#9CFF9CResolved Reports");
        boolean canRemove = staff.hasPermission(REMOVE_RESOLVED_PERMISSION);
        for (Report report : reportService.resolved()) {
            menu.addItem(buildResolvedReportItem(report, canRemove), event -> {
                if (event.getClick().isRightClick() && canRemove) {
                    reportService.delete(report.id());
                    staff.sendMessage(MythicHex.colorize(
                            "&#9CFF9CRemoved resolved report #" + report.id() + "."));
                    openResolved(staff, 0);
                }
            });
        }
        menu.staticSlot(49, MythicItem.create(Material.BARRIER).name("&#FF8A8ABack").build(),
                event -> openOverview(staff));
        menu.open(staff, page);
    }

    @NotNull
    private ItemStack buildActiveReportItem(@NotNull Report report) {
        String targetServer = lookupServerFor(report.targetUuid(), report.targetName(),
                report.targetServerCache(), localShardId);
        return MythicItem.create(Material.PAPER)
                .name("&#FF8A8A" + report.targetName() + " &7— &#FFFFFF" + report.category().displayName())
                .lore(
                        "&7Reported by: &f" + report.reporterName(),
                        "&7Submitted: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(report.submittedAt())),
                        "&7Reporter server: &f" + networkLabel(report.reporterServer()),
                        "&7Target server: &f" + targetServer,
                        "&7Report id: &f#" + report.id(),
                        "",
                        "&#9CFF9CRight-click to mark resolved.")
                .build();
    }

    @NotNull
    private ItemStack buildResolvedReportItem(@NotNull Report report, boolean canRemove) {
        MythicItem item = MythicItem.create(Material.WRITTEN_BOOK)
                .name("&#9CFF9C" + report.targetName() + " &7— &#FFFFFF" + report.category().displayName())
                .lore(
                        "&7Reported by: &f" + report.reporterName(),
                        "&7Resolved by: &f" + (report.resolverName() == null ? "?" : report.resolverName()),
                        "&7Resolution: &f" + (report.resolution() == null ? "" : report.resolution()),
                        "&7Resolved at: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(report.resolvedAt())),
                        "&7Submitted: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(report.submittedAt())),
                        "&7Report id: &f#" + report.id(),
                        "",
                        canRemove
                                ? "&#FF8A8ARight-click to remove from log."
                                : "&8(Admins can remove resolved reports.)");
        return item.build();
    }

    private void promptResolution(@NotNull Player staff, @NotNull Report report) {
        staff.closeInventory();
        staff.sendMessage(MythicHex.colorize(
                "&#D2D8E0Enter resolution for report &#FFFFFF#" + report.id()
                        + " &#D2D8E0(or type &#FFFFFFcancel&#D2D8E0)."));
        prompts.await(staff, (p, input) -> {
            String resolution = input.trim();
            if (resolution.isEmpty()) {
                p.sendMessage(MythicHex.colorize("&#FF8A8AResolution cannot be empty."));
                return;
            }
            if (!reportService.resolve(report.id(), p.getUniqueId(), p.getName(), resolution)) {
                p.sendMessage(MythicHex.colorize("&#FF8A8AThat report is no longer active."));
                return;
            }
            p.sendMessage(MythicHex.colorize(
                    "&#9CFF9CReport &#FFFFFF#" + report.id() + " &#9CFF9Cmarked resolved."));
            var plug = (org.bukkit.plugin.java.JavaPlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("MythicCore");
            if (plug != null) {
                net.mythicpvp.suite.scheduler.MythicScheduler.runLater(plug, () -> openActive(p, 0), 5L);
            }
        });
    }

    @NotNull
    private String lookupServerFor(@NotNull UUID targetUuid, @NotNull String targetName,
                                    String fallback, @NotNull String localShardId) {
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null && online.isOnline()) {
            return networkLabel(localShardId);
        }
        if (presence != null) {
            String shardId = presence.shardOf(targetName);
            if (shardId != null) {
                return networkLabel(shardId);
            }
        }
        return fallback == null ? "offline" : fallback;
    }

    @NotNull
    private String networkLabel(@NotNull String shardId) {
        if (shardRegistry != null) {
            for (var row : shardRegistry.all()) {
                if (shardId.equalsIgnoreCase(row.shard_id())) {
                    String role = row.role();
                    if (role != null && !role.isEmpty()) {
                        String lower = role.toLowerCase(java.util.Locale.ROOT);
                        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
                    }
                }
            }
        }
        int dash = shardId.indexOf('-');
        String stem = dash > 0 ? shardId.substring(0, dash) : shardId;
        if (stem.isEmpty()) return shardId;
        String lower = stem.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

}

package net.mythicpvp.core.punishment;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public final class PunishmentMenuService {

    private final PunishmentService punishmentService;
    private final ChatPromptService prompts;
    private final Clock clock;
    private final String serverId;
    private final PunishmentMenuText text;

    public PunishmentMenuService(@NotNull PunishmentService punishmentService, @NotNull ChatPromptService prompts, @NotNull Clock clock, @NotNull String serverId) {

        this(punishmentService, prompts, clock, serverId, PunishmentMenuText.DEFAULTS);
    }

    public PunishmentMenuService(@NotNull PunishmentService punishmentService, @NotNull ChatPromptService prompts,
                                 @NotNull Clock clock, @NotNull String serverId,
                                 @NotNull PunishmentMenuText text) {
        this.punishmentService = punishmentService;
        this.prompts = prompts;
        this.clock = clock;
        this.serverId = serverId;
        this.text = text;
    }

    public void openPunish(@NotNull Player staff, @NotNull PunishmentFlow flow) {
        MythicMenu menu = MythicMenu.create(3, text.punishTitle(flow.targetName()));
        int[] slots = {10, 12, 14, 16};
        PunishmentCategory[] categories = PunishmentCategory.values();
        for (int i = 0; i < categories.length; i++) {
            PunishmentCategory category = categories[i];
            menu.slot(slots[i], MythicItem.create(category.material())
                    .name(text.categoryName(category.name()))
                    .lore(text.categoryLoreTemplates()).build(),
                    event -> openTemplates(staff, flow, category, true));
        }
        menu.open(staff);
    }

    public void openHandbook(@NotNull Player staff) {
        MythicMenu menu = MythicMenu.create(3, text.handbookTitle());
        int[] slots = {10, 12, 14, 16};
        PunishmentCategory[] categories = PunishmentCategory.values();
        for (int i = 0; i < categories.length; i++) {
            PunishmentCategory category = categories[i];
            menu.slot(slots[i], MythicItem.create(category.material())
                    .name(text.categoryName(category.name()))
                    .lore(text.categoryLoreHandbook()).build(),
                    event -> openTemplates(staff, null, category, false));
        }
        menu.open(staff);
    }

    public void openTemplates(@NotNull Player staff, PunishmentFlow flow, @NotNull PunishmentCategory category, boolean executable) {
        PaginatedMenu menu = PaginatedMenu.create(6, text.templatesTitle(category.name()));
        if (!executable) {
            menu.staticSlot(49, MythicItem.create(Material.ARROW)
                    .name("&#F529BEBack")
                    .build(), event -> openHandbook(staff));
        }
        for (PunishmentTemplate template : punishmentService.templates(category)) {
            menu.addItem(MythicItem.create(category.material())
                    .name("&#F529BE" + template.title())
                    .lore(templateLore(template, executable))
                    .build(), event -> {
                        if (executable && flow != null) {
                            openProof(staff, flow.template(template));
                        }
                    });
        }
        menu.open(staff);
    }

    public void openProof(@NotNull Player staff, @NotNull PunishmentFlow flow) {
        MythicMenu.create(3, text.proofTitle())
                .slot(11, MythicItem.create(Material.PAPER).name(text.enterProofName()).lore(text.enterProofLore()).build(),
                        event -> prompts.await(staff, (player, input) -> openProof(player, flow.proof(input))))
                .slot(13, MythicItem.create(Material.BOOK).name(text.proofSummaryName()).lore(flow.proof().isBlank() ? List.of(text.noProofYet()) : List.of("&7" + flow.proof())).build())
                .slot(15, MythicItem.create(flow.proof().isBlank() ? Material.RED_WOOL : Material.LIME_WOOL).name(flow.proof().isBlank() ? text.noProofButton() : text.confirmProof()).build(), event -> {
                    if (!flow.proof().isBlank()) {
                        openConfirm(staff, flow);
                    }
                })
                .open(staff);
    }

    public void openConfirm(@NotNull Player staff, @NotNull PunishmentFlow flow) {
        MythicMenu.create(3, text.confirmTitle())
                .slot(10, MythicItem.create(flow.clearInventory() ? Material.LIME_DYE : Material.GRAY_DYE).name(text.clearInventoryName()).lore(text.statePrefix() + yesNo(flow.clearInventory()), text.toggleHint()).build(), event -> openConfirm(staff, flow.clearInventory(!flow.clearInventory())))
                .slot(12, MythicItem.create(flow.silent() ? Material.GRAY_DYE : Material.LIME_DYE).name(text.silentName()).lore(text.statePrefix() + yesNo(flow.silent()), text.toggleHint()).build(), event -> openConfirm(staff, flow.silent(!flow.silent())))
                .slot(13, MythicItem.create(Material.BOOK).name(text.summaryName()).lore(summary(flow)).build())
                .slot(16, MythicItem.create(Material.LIME_WOOL).name(text.executeName()).build(), event -> execute(staff, flow))
                .open(staff);
    }

    public void openHistory(@NotNull Player viewer, @NotNull String targetName, @NotNull List<PunishmentRecord> history) {
        openHistoryFiltered(viewer, targetName, history, null, ActivityFilter.ALL);
    }

    private void openHistoryFiltered(@NotNull Player viewer, @NotNull String targetName,
                                     @NotNull List<PunishmentRecord> history,
                                     PunishmentType typeFilter,
                                     @NotNull ActivityFilter activityFilter) {
        PaginatedMenu menu = PaginatedMenu.create(6, text.historyTitle(targetName));
        long now = System.currentTimeMillis();
        for (PunishmentRecord record : history) {
            if (typeFilter != null && record.type() != typeFilter) continue;
            if (!activityFilter.matches(record, now)) continue;
            menu.addItem(MythicItem.create(record.pardoned() ? Material.GRAY_DYE : Material.RED_DYE)
                    .name("&#F529BE" + record.type().name())
                    .lore(recordLore(record))
                    .build());
        }
        String typeLabel = typeFilter == null ? "All types" : typeFilter.name();
        menu.staticSlot(47, MythicItem.create(Material.HOPPER)
                        .name("&#FFEC8AFilter: type &7— &#FFFFFF" + typeLabel)
                        .lore(
                                "&7Cycle through punishment types.",
                                "&#9CFF9CClick to change.")
                        .build(),
                event -> openHistoryFiltered(viewer, targetName, history, cycleType(typeFilter), activityFilter));
        menu.staticSlot(51, MythicItem.create(Material.COMPARATOR)
                        .name("&#FFEC8AFilter: status &7— &#FFFFFF" + activityFilter.label)
                        .lore(
                                "&7Toggle active / inactive / all.",
                                "&#9CFF9CClick to change.")
                        .build(),
                event -> openHistoryFiltered(viewer, targetName, history, typeFilter, activityFilter.next()));
        menu.open(viewer);
    }

    private static PunishmentType cycleType(PunishmentType current) {
        PunishmentType[] order = PunishmentType.values();
        if (current == null) return order[0];
        int idx = current.ordinal() + 1;
        return idx >= order.length ? null : order[idx];
    }

    public enum ActivityFilter {
        ALL("All"),
        ACTIVE("Active only"),
        INACTIVE("Inactive only");

        public final String label;

        ActivityFilter(String label) {
            this.label = label;
        }

        ActivityFilter next() {
            return values()[(ordinal() + 1) % values().length];
        }

        boolean matches(@NotNull PunishmentRecord record, long now) {
            return switch (this) {
                case ALL -> true;
                case ACTIVE -> record.active(now);
                case INACTIVE -> !record.active(now);
            };
        }
    }

    private void execute(@NotNull Player staff, @NotNull PunishmentFlow flow) {
        PunishmentTemplate template = flow.template();
        PunishmentRecord record = punishmentService.punish(new PunishmentRequest(flow.targetUuid(), flow.targetName(), staff.getUniqueId(), staff.getName(), template.type(), template.title(), flow.proof(), template.expiresAt(clock.instant()), flow.silent(), flow.clearInventory(), serverId));
        staff.closeInventory();
        staff.sendMessage(MythicHex.colorize("&#9CFF9CPunishment executed: &f" + record.type().name() + " " + record.targetName() + "&#9CFF9C."));
    }

    @NotNull
    private List<String> templateLore(@NotNull PunishmentTemplate template, boolean executable) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Category: &f" + template.category().name());
        lore.add("&7Duration: &f" + template.duration());
        lore.add("&7Info: &f" + template.information());
        lore.add(text.templateClickHint(executable));
        return lore;
    }

    @NotNull
    private static List<String> summary(@NotNull PunishmentFlow flow) {
        PunishmentTemplate template = flow.template();
        return List.of(
                "&7Target: &f" + flow.targetName(),
                "&7Type: &f" + template.type().name(),
                "&7Duration: &f" + template.duration(),
                "&7Reason: &f" + template.title(),
                "&7Proof: &f" + flow.proof(),
                "&7Silent: &f" + yesNo(flow.silent()),
                "&7Clear Inventory: &f" + yesNo(flow.clearInventory())
        );
    }

    @NotNull
    private static List<String> recordLore(@NotNull PunishmentRecord record) {
        return List.of(
                "&7Reason: &f" + record.reason(),
                "&7Proof: &f" + (record.proof().isBlank() ? "None" : record.proof()),
                "&7Executor: &f" + record.staffName(),
                "&7Server: &f" + record.server(),
                "&7Silent: &f" + yesNo(record.silent()),
                "&7Clear Inventory: &f" + yesNo(record.clearInventory()),
                "&7State: &f" + (record.pardoned() ? "Pardoned" : "Active/Expired")
        );
    }

    @NotNull
    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}

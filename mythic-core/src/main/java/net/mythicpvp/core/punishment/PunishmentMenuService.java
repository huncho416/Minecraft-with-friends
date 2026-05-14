package net.mythicpvp.core.punishment;

import net.mythicpvp.core.prompt.ChatPromptService;
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
        // Back-compat: tests + callers without a menus.yml use defaults.
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
        for (PunishmentTemplate template : punishmentService.templates(category)) {
            menu.addItem(MythicItem.create(category.material())
                    .name("&#FF00F8" + template.title())
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
        PaginatedMenu menu = PaginatedMenu.create(6, text.historyTitle(targetName));
        for (PunishmentRecord record : history) {
            menu.addItem(MythicItem.create(record.pardoned() ? Material.GRAY_DYE : Material.RED_DYE)
                    .name("&#FF00F8" + record.type().name())
                    .lore(recordLore(record))
                    .build());
        }
        menu.open(viewer);
    }

    private void execute(@NotNull Player staff, @NotNull PunishmentFlow flow) {
        PunishmentTemplate template = flow.template();
        PunishmentRecord record = punishmentService.punish(new PunishmentRequest(flow.targetUuid(), flow.targetName(), staff.getUniqueId(), staff.getName(), template.type(), template.title(), flow.proof(), template.expiresAt(clock.instant()), flow.silent(), flow.clearInventory(), serverId));
        staff.closeInventory();
        staff.sendMessage("Punishment executed: " + record.type().name() + " " + record.targetName() + ".");
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

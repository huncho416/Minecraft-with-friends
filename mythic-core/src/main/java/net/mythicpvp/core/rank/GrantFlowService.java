package net.mythicpvp.core.rank;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GrantFlowService {

    private final RankService rankService;
    private final GrantService grantService;
    private final ChatPromptService prompts;
    private final RankMenuText text;

    public GrantFlowService(@NotNull RankService rankService, @NotNull GrantService grantService, @NotNull ChatPromptService prompts) {

        this(rankService, grantService, prompts, RankMenuText.DEFAULTS);
    }

    public GrantFlowService(@NotNull RankService rankService, @NotNull GrantService grantService,
                            @NotNull ChatPromptService prompts, @NotNull RankMenuText text) {
        this.rankService = rankService;
        this.grantService = grantService;
        this.prompts = prompts;
        this.text = text;
    }

    public void openRankSelection(@NotNull Player executor, @NotNull GrantFlow flow) {
        PaginatedMenu menu = PaginatedMenu.create(6, text.grantRankTitle(flow.targetName()));
        for (CoreRank rank : rankService.all()) {
            menu.addItem(MythicItem.create(rank.dye())
                    .name(rank.color() + rank.name())
                    .lore(List.of(
                            "&7Staff Rank: &f" + yesNo(rank.staff()),
                            "&7Purchaseable: &f" + yesNo(rank.donator()),
                            "&7Prefix: &f" + rank.prefix(),
                            "&7Parent: &f" + (rank.parent().isBlank() ? "None" : rank.parent()),
                            "&7Weight: &f" + rank.weight(),
                            "&7Permissions: &f" + rank.permissions().size(),
                            text.clickToSelect()
                    ))
                    .build(), event -> openDuration(executor, flow.rank(rank.id())));
        }
        menu.open(executor);
    }

    public void openDuration(@NotNull Player executor, @NotNull GrantFlow flow) {
        MythicMenu menu = MythicMenu.create(3, text.grantDurationTitle());

        int[] slots = {10, 11, 12, 13, 14, 15};
        List<String> durations = text.durationPresets();
        int count = Math.min(slots.length, durations.size());
        for (int i = 0; i < count; i++) {
            String duration = durations.get(i);
            menu.slot(slots[i], MythicItem.create(Material.CLOCK).name("&#F529BE" + duration).build(),
                    event -> openReason(executor, flow.duration(GrantDuration.parse(duration))));
        }
        menu.slot(16, MythicItem.create(Material.PAPER).name(text.custom()).build(),
                event -> prompts.await(executor, (player, input) -> openReason(player, flow.duration(GrantDuration.parse(input)))));
        menu.open(executor);
    }

    public void openReason(@NotNull Player executor, @NotNull GrantFlow flow) {
        MythicMenu menu = MythicMenu.create(3, text.grantReasonTitle());
        int[] slots = {11, 13, 15};
        List<String> reasons = text.reasonPresets();
        int count = Math.min(slots.length, reasons.size());
        for (int i = 0; i < count; i++) {
            String reason = reasons.get(i);
            menu.slot(slots[i], MythicItem.create(Material.BOOK).name("&#F529BE" + reason).build(),
                    event -> openConfirm(executor, flow.reason(reason)));
        }
        menu.slot(22, MythicItem.create(Material.PAPER).name(text.custom()).build(),
                event -> prompts.await(executor, (player, input) -> openConfirm(player, flow.reason(input))));
        menu.open(executor);
    }

    public void openConfirm(@NotNull Player executor, @NotNull GrantFlow flow) {
        MythicMenu.create(3, text.grantConfirmTitle())
                .slot(11, MythicItem.create(Material.BOOK).name(text.summary()).lore(List.of(
                        "&7Target: &f" + flow.targetName(),
                        "&7Rank: &f" + flow.rankId(),
                        "&7Duration: &f" + flow.duration().input(),
                        "&7Reason: &f" + flow.reason())).build())
                .slot(13, MythicItem.create(Material.LIME_WOOL).name(text.confirm()).build(), event -> {
                    grantService.grant(flow.targetUuid(), flow.targetName(), flow.rankId(), flow.duration(), executor.getUniqueId(), executor.getName(), flow.reason());
                    executor.closeInventory();
                    executor.sendMessage("Granted " + flow.rankId() + " to " + flow.targetName() + ".");
                })
                .slot(15, MythicItem.create(Material.RED_WOOL).name(text.cancel()).build(), event -> executor.closeInventory())
                .open(executor);
    }

    @NotNull
    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}

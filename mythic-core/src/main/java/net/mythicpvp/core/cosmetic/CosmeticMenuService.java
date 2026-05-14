package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.economy.EconomyManager;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class CosmeticMenuService {

    private final CosmeticService cosmeticService;
    private final CrateService crateService;
    private final CosmeticMenuText text;

    public CosmeticMenuService(@NotNull CosmeticService cosmeticService,
                               @NotNull CrateService crateService,
                               @NotNull CosmeticMenuText text) {
        this.cosmeticService = cosmeticService;
        this.crateService = crateService;
        this.text = text;
    }

    public void openMain(@NotNull Player player) {
        MythicMenu menu = MythicMenu.create(3, text.mainTitle());

        Material[] typeMaterials = {
                Material.LEATHER_HELMET, Material.NAME_TAG, Material.BLAZE_POWDER,
                Material.DIAMOND_SWORD, Material.FIREWORK_ROCKET, Material.PAPER
        };
        CosmeticType[] types = CosmeticType.values();
        int[] slots = {10, 11, 12, 13, 14, 15};

        for (int i = 0; i < types.length && i < slots.length; i++) {
            CosmeticType type = types[i];
            Material mat = i < typeMaterials.length ? typeMaterials[i] : Material.CHEST;
            long ownedCount = CosmeticManager.getInstance().getByType(type).stream()
                    .filter(c -> CosmeticManager.getInstance().ownsCosmetic(player.getUniqueId(), c.id()))
                    .count();
            long totalCount = CosmeticManager.getInstance().getByType(type).size();

            menu.slot(slots[i], MythicItem.create(mat)
                    .name("&#F529BE" + type.getDisplayName())
                    .lore(List.of("&7Owned: &f" + ownedCount + "/" + totalCount, text.clickToView()))
                    .build(), event -> openBrowse(player, type));
        }

        menu.slot(22, MythicItem.create(Material.ENDER_CHEST)
                .name(text.openCrates())
                .lore(List.of("&7Open cosmetic crates"))
                .build(), event -> openCrates(player));

        menu.open(player);
    }

    public void openBrowse(@NotNull Player player, @NotNull CosmeticType type) {
        UUID uuid = player.getUniqueId();
        PaginatedMenu menu = PaginatedMenu.create(6, text.browseTitle(type.getDisplayName()));

        Collection<CosmeticManager.Cosmetic> cosmetics = CosmeticManager.getInstance().getByType(type);
        for (CosmeticManager.Cosmetic cosmetic : cosmetics) {
            boolean owns = CosmeticManager.getInstance().ownsCosmetic(uuid, cosmetic.id());
            String equippedId = CosmeticManager.getInstance().getEquipped(uuid, type);
            boolean isEquipped = cosmetic.id().equalsIgnoreCase(equippedId);

            String status = isEquipped ? text.equipped() : (owns ? text.owned() : text.notOwned());

            menu.addItem(MythicItem.create(owns ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name("&#F529BE" + cosmetic.displayName())
                    .lore(List.of(
                            "&7Rarity: &f" + cosmetic.rarity(),
                            "&7" + cosmetic.description(),
                            "&7Status: " + status,
                            owns ? text.clickToView() : ""))
                    .build(), event -> {
                if (owns) openDetail(player, cosmetic);
            });
        }

        menu.open(player);
    }

    public void openDetail(@NotNull Player player, @NotNull CosmeticManager.Cosmetic cosmetic) {
        UUID uuid = player.getUniqueId();
        String equippedId = CosmeticManager.getInstance().getEquipped(uuid, cosmetic.type());
        boolean isEquipped = cosmetic.id().equalsIgnoreCase(equippedId);

        MythicMenu menu = MythicMenu.create(3, text.detailTitle(cosmetic.displayName()));

        menu.slot(11, MythicItem.create(Material.BOOK)
                .name("&#F529BE" + cosmetic.displayName())
                .lore(List.of(
                        "&7Type: &f" + cosmetic.type().getDisplayName(),
                        "&7Rarity: &f" + cosmetic.rarity(),
                        "&7" + cosmetic.description(),
                        "&7Tradable: &f" + (cosmetic.tradable() ? "Yes" : "No"),
                        "&7Limited: &f" + (cosmetic.limited() ? "Yes" : "No")))
                .build());

        if (isEquipped) {
            menu.slot(13, MythicItem.create(Material.RED_WOOL)
                    .name(text.clickToUnequip())
                    .build(), event -> {
                cosmeticService.unequip(uuid, cosmetic.type());
                player.closeInventory();
                player.sendMessage("Unequipped " + cosmetic.displayName() + ".");
            });
        } else {
            menu.slot(13, MythicItem.create(Material.LIME_WOOL)
                    .name(text.clickToEquip())
                    .build(), event -> {
                cosmeticService.equip(uuid, cosmetic.type(), cosmetic.id());
                player.closeInventory();
                player.sendMessage("Equipped " + cosmetic.displayName() + ".");
            });
        }

        if (cosmetic.tradable()) {
            menu.slot(15, MythicItem.create(Material.CHEST)
                    .name(text.withdraw())
                    .build(), event -> {
                ItemStack item = cosmeticService.withdraw(uuid, cosmetic.id());
                if (item != null) {
                    player.getInventory().addItem(item);
                    player.closeInventory();
                    player.sendMessage("Withdrew " + cosmetic.displayName() + " as an item.");
                } else {
                    player.sendMessage("Cannot withdraw this cosmetic.");
                }
            });
        }

        menu.open(player);
    }

    public void openCrates(@NotNull Player player) {
        PaginatedMenu menu = PaginatedMenu.create(3, text.cratesTitle());
        UUID uuid = player.getUniqueId();

        for (CrateDefinition crate : crateService.allCrates()) {
            long balance = EconomyManager.getInstance().getBalance(uuid, crate.currency());
            menu.addItem(MythicItem.create(Material.ENDER_CHEST)
                    .name("&#F529BE" + crate.displayName())
                    .lore(List.of(
                            "&7Cost: &f" + crate.cost() + " " + crate.currency().getDisplayName(),
                            "&7Your " + crate.currency().getDisplayName() + ": &f" + balance,
                            "&7Items: &f" + crate.entries().size(),
                            "&#D2D8E0Click to open"))
                    .build(), event -> openCrateConfirm(player, crate));
        }

        menu.open(player);
    }

    public void openCrateConfirm(@NotNull Player player, @NotNull CrateDefinition crate) {
        MythicMenu menu = MythicMenu.create(3, text.crateConfirmTitle(crate.displayName()));

        menu.slot(11, MythicItem.create(Material.BOOK)
                .name("&#F529BE" + crate.displayName())
                .lore(List.of(
                        "&7Cost: &f" + crate.cost() + " " + crate.currency().getDisplayName(),
                        "&7Items in pool: &f" + crate.entries().size()))
                .build());

        menu.slot(13, MythicItem.create(Material.LIME_WOOL)
                .name(text.confirm())
                .build(), event -> {
            CrateRoll roll = crateService.roll(player.getUniqueId(), crate);
            player.closeInventory();
            if (roll != null) {
                CosmeticManager.Cosmetic cosmetic = CosmeticManager.getInstance().get(roll.cosmeticId());
                String name = cosmetic != null ? cosmetic.displayName() : roll.cosmeticId();
                player.sendMessage("You opened " + crate.displayName() + " and received " + name + "!");
            } else {
                player.sendMessage("Insufficient " + crate.currency().getDisplayName() + " to open this crate.");
            }
        });

        menu.slot(15, MythicItem.create(Material.RED_WOOL)
                .name(text.cancel())
                .build(), event -> openCrates(player));

        menu.open(player);
    }
}

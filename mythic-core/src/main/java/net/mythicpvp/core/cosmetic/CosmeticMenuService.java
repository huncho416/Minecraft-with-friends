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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

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
                Material.DIAMOND_SWORD, Material.FIREWORK_ROCKET, Material.PAPER, Material.INK_SAC
        };
        CosmeticType[] types = CosmeticType.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16};

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
                    .build(), event -> openBrowse(player, type, EnumSet.noneOf(BrowseFilter.class)));
        }

        menu.slot(22, MythicItem.create(Material.ENDER_CHEST)
                .name(text.openCrates())
                .lore(List.of("&7Open cosmetic crates"))
                .build(), event -> openCrates(player));

        menu.open(player);
    }

    public void openBrowse(@NotNull Player player, @NotNull CosmeticType type,
                           @NotNull Set<BrowseFilter> activeFilters) {
        UUID uuid = player.getUniqueId();
        PaginatedMenu menu = PaginatedMenu.create(6, text.browseTitle(type.getDisplayName()));

        Collection<CosmeticManager.Cosmetic> cosmetics = CosmeticManager.getInstance().getByType(type);
        Stream<CosmeticManager.Cosmetic> stream = cosmetics.stream();

        if (activeFilters.contains(BrowseFilter.OWNED)) {
            stream = stream.filter(c -> CosmeticManager.getInstance().ownsCosmetic(uuid, c.id()));
        }
        if (activeFilters.contains(BrowseFilter.NOT_OWNED)) {
            stream = stream.filter(c -> !CosmeticManager.getInstance().ownsCosmetic(uuid, c.id()));
        }
        if (activeFilters.contains(BrowseFilter.EQUIPPED)) {
            stream = stream.filter(c -> {
                String eq = CosmeticManager.getInstance().getEquipped(uuid, c.type());
                return c.id().equalsIgnoreCase(eq);
            });
        }
        if (activeFilters.contains(BrowseFilter.TRADABLE)) {
            stream = stream.filter(CosmeticManager.Cosmetic::tradable);
        }
        if (activeFilters.contains(BrowseFilter.LIMITED)) {
            stream = stream.filter(CosmeticManager.Cosmetic::limited);
        }
        if (activeFilters.contains(BrowseFilter.COMMON)) {
            stream = stream.filter(c -> "COMMON".equalsIgnoreCase(c.rarity()));
        }
        if (activeFilters.contains(BrowseFilter.RARE)) {
            stream = stream.filter(c -> "RARE".equalsIgnoreCase(c.rarity()));
        }
        if (activeFilters.contains(BrowseFilter.EPIC)) {
            stream = stream.filter(c -> "EPIC".equalsIgnoreCase(c.rarity()));
        }
        if (activeFilters.contains(BrowseFilter.LEGENDARY)) {
            stream = stream.filter(c -> "LEGENDARY".equalsIgnoreCase(c.rarity()));
        }

        List<CosmeticManager.Cosmetic> filtered = stream.toList();
        for (CosmeticManager.Cosmetic cosmetic : filtered) {
            boolean owns = CosmeticManager.getInstance().ownsCosmetic(uuid, cosmetic.id());
            String equippedId = CosmeticManager.getInstance().getEquipped(uuid, type);
            boolean isEquipped = cosmetic.id().equalsIgnoreCase(equippedId);

            String status = isEquipped ? text.equipped() : (owns ? text.owned() : text.notOwned());
            String preview = previewFor(cosmetic);

            List<String> lore = new java.util.ArrayList<>();
            lore.add("&7Rarity: &f" + cosmetic.rarity());
            lore.add("&7" + cosmetic.description());
            if (preview != null) {
                lore.add("&7Preview: " + preview);
            }
            lore.add("&7Status: " + status);
            if (owns) {
                lore.add(text.clickToView());
            }

            menu.addItem(MythicItem.create(owns ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name("&#F529BE" + cosmetic.displayName())
                    .lore(lore)
                    .build(), event -> {
                if (owns) openDetail(player, cosmetic);
            });
        }

        addFilterBar(menu, player, type, activeFilters);
        menu.open(player);
    }

    private void addFilterBar(@NotNull PaginatedMenu menu, @NotNull Player player,
                              @NotNull CosmeticType type, @NotNull Set<BrowseFilter> active) {
        int slot = 45;
        for (BrowseFilter filter : BrowseFilter.values()) {
            if (slot > 52) break;
            boolean on = active.contains(filter);
            Material mat = on ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            String toggle = on ? "&aON" : "&7OFF";
            menu.staticSlot(slot, MythicItem.create(mat)
                    .name("&#F529BE" + filter.displayName)
                    .lore(List.of("&7Filter: " + toggle, "&#D2D8E0Click to toggle"))
                    .build(), event -> {
                Set<BrowseFilter> next = EnumSet.copyOf(active.isEmpty() ? EnumSet.noneOf(BrowseFilter.class) : active);
                if (next.contains(filter)) {
                    next.remove(filter);
                } else {
                    next.add(filter);
                }
                openBrowse(player, type, next);
            });
            slot++;
        }
    }

    public void openDetail(@NotNull Player player, @NotNull CosmeticManager.Cosmetic cosmetic) {
        UUID uuid = player.getUniqueId();
        String equippedId = CosmeticManager.getInstance().getEquipped(uuid, cosmetic.type());
        boolean isEquipped = cosmetic.id().equalsIgnoreCase(equippedId);

        MythicMenu menu = MythicMenu.create(3, text.detailTitle(cosmetic.displayName()));

        String preview = previewFor(cosmetic);
        List<String> infoLore = new java.util.ArrayList<>();
        infoLore.add("&7Type: &f" + cosmetic.type().getDisplayName());
        infoLore.add("&7Rarity: &f" + cosmetic.rarity());
        infoLore.add("&7" + cosmetic.description());
        if (preview != null) {
            infoLore.add("&7Preview: " + preview);
        }
        infoLore.add("&7Tradable: &f" + (cosmetic.tradable() ? "Yes" : "No"));
        infoLore.add("&7Limited: &f" + (cosmetic.limited() ? "Yes" : "No"));

        menu.slot(11, MythicItem.create(Material.BOOK)
                .name("&#F529BE" + cosmetic.displayName())
                .lore(infoLore)
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
            if (!crate.isAvailable()) continue;
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

    @org.jetbrains.annotations.Nullable
    private String previewFor(@NotNull CosmeticManager.Cosmetic cosmetic) {
        String format = cosmetic.format();
        if (format == null || format.isBlank()) {
            return null;
        }
        if (cosmetic.type() == CosmeticType.CHAT_COLOR) {
            return format.replace("%message%", "Hello world");
        }
        return format;
    }

    public enum BrowseFilter {
        OWNED("Owned"),
        NOT_OWNED("Not Owned"),
        EQUIPPED("Equipped"),
        TRADABLE("Tradable"),
        LIMITED("Limited"),
        COMMON("Common"),
        RARE("Rare"),
        EPIC("Epic"),
        LEGENDARY("Legendary");

        final String displayName;

        BrowseFilter(@NotNull String displayName) {
            this.displayName = displayName;
        }
    }
}

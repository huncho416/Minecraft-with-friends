package net.mythicpvp.core.credit;

import net.mythicpvp.core.cosmetic.CosmeticService;
import net.mythicpvp.core.cosmetic.CrateDefinition;
import net.mythicpvp.core.cosmetic.CrateService;
import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CreditShopService {

    private static final UUID CONSOLE_UUID = new UUID(0, 0);
    private static final String CONSOLE_NAME = "Credit Shop";

    private final CreditService creditService;
    private final GrantService grantService;
    private final CosmeticService cosmeticService;
    private final CrateService crateService;
    private final CreditShopText text;
    private final List<ShopCategory> categories = new ArrayList<>();

    public CreditShopService(@NotNull CreditService creditService,
                             @NotNull GrantService grantService,
                             @NotNull CosmeticService cosmeticService,
                             @NotNull CrateService crateService,
                             @NotNull CreditShopText text) {
        this.creditService = creditService;
        this.grantService = grantService;
        this.cosmeticService = cosmeticService;
        this.crateService = crateService;
        this.text = text;
    }

    public void loadFromConfig(@NotNull MythicConfig config) {
        categories.clear();
        ConfigurationSection root = config.getConfig().getConfigurationSection("shop.categories");
        if (root == null) return;

        for (String catId : root.getKeys(false)) {
            ConfigurationSection catSection = root.getConfigurationSection(catId);
            if (catSection == null) continue;

            String displayName = catSection.getString("display-name", catId);
            Material material;
            try {
                material = Material.valueOf(catSection.getString("material", "CHEST"));
            } catch (IllegalArgumentException e) {
                material = Material.CHEST;
            }
            int slot = catSection.getInt("slot", 13);

            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection itemsSection = catSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) continue;

                    String itemDisplayName = itemSection.getString("display-name", itemId);
                    Material itemMat;
                    try {
                        itemMat = Material.valueOf(itemSection.getString("material", "PAPER"));
                    } catch (IllegalArgumentException e) {
                        itemMat = Material.PAPER;
                    }
                    long cost = itemSection.getLong("cost", 0);
                    ShopItem.ShopItemType type;
                    try {
                        type = ShopItem.ShopItemType.valueOf(itemSection.getString("type", "COSMETIC"));
                    } catch (IllegalArgumentException e) {
                        type = ShopItem.ShopItemType.COSMETIC;
                    }
                    String value = itemSection.getString("value", "");
                    String requiresRank = itemSection.getString("requires-rank");
                    List<String> lore = itemSection.getStringList("lore");

                    items.add(new ShopItem(itemId, itemDisplayName, itemMat, cost, type, value, requiresRank, lore));
                }
            }

            categories.add(new ShopCategory(catId, displayName, material, slot, List.copyOf(items)));
        }
    }

    public void openMain(@NotNull Player player) {
        MythicMenu menu = MythicMenu.create(3, text.shopTitle());
        UUID uuid = player.getUniqueId();
        long balance = creditService.getBalance(uuid);

        menu.slot(4, MythicItem.create(Material.GOLD_INGOT)
                .name("&#FFD700Your Credits")
                .lore(List.of("&7Balance: &f" + balance))
                .build());

        for (ShopCategory category : categories) {
            int itemCount = category.items().size();
            menu.slot(category.slot(), MythicItem.create(category.material())
                    .name("&#F529BE" + category.displayName())
                    .lore(List.of("&7Items: &f" + itemCount, text.clickToView()))
                    .build(), event -> openCategory(player, category));
        }

        menu.open(player);
    }

    public void openCategory(@NotNull Player player, @NotNull ShopCategory category) {
        openCategory(player, category, 0);
    }

    public void openCategory(@NotNull Player player, @NotNull ShopCategory category, int page) {
        UUID uuid = player.getUniqueId();
        long balance = creditService.getBalance(uuid);
        PaginatedMenu menu = PaginatedMenu.create(6, text.categoryTitle(category.displayName()));

        for (ShopItem item : category.items()) {
            boolean canAfford = balance >= item.cost();
            List<String> lore = new ArrayList<>(item.lore());
            lore.add("");
            lore.add("&7Cost: &#FFD700" + item.cost() + " Credits");
            lore.add(canAfford ? "&#9CFF9CClick to purchase" : "&#FF8A8AInsufficient credits");

            menu.addItem(MythicItem.create(item.material())
                    .name("&#F529BE" + item.displayName())
                    .lore(lore)
                    .build(), event -> openConfirm(player, category, item));
        }

        menu.staticSlot(49, MythicItem.create(Material.BARRIER)
                        .name("&#FF8A8ABack")
                        .lore(List.of("&7Return to the shop main menu."))
                        .build(),
                event -> openMain(player));
        menu.open(player, page);
    }

    public void openConfirm(@NotNull Player player, @NotNull ShopCategory category, @NotNull ShopItem item) {
        UUID uuid = player.getUniqueId();
        long balance = creditService.getBalance(uuid);

        MythicMenu menu = MythicMenu.create(3, text.confirmTitle(item.displayName()));

        List<String> lore = new ArrayList<>(item.lore());
        lore.add("");
        lore.add("&7Cost: &#FFD700" + item.cost() + " Credits");
        lore.add("&7Your Credits: &f" + balance);

        menu.slot(11, MythicItem.create(item.material())
                .name("&#F529BE" + item.displayName())
                .lore(lore)
                .build());

        menu.slot(13, MythicItem.create(Material.LIME_WOOL)
                .name(text.confirm())
                .build(), event -> {
            boolean success = executePurchase(player, item);
            player.closeInventory();
            if (success) {
                player.sendMessage("&#9CFF9CPurchased " + item.displayName() + "!");
            } else {
                player.sendMessage("&#FF8A8APurchase failed. Check your credits or requirements.");
            }
        });

        menu.slot(15, MythicItem.create(Material.RED_WOOL)
                .name(text.cancel())
                .build(), event -> openCategory(player, category));

        menu.open(player);
    }

    private boolean executePurchase(@NotNull Player player, @NotNull ShopItem item) {
        UUID uuid = player.getUniqueId();

        if (item.requiresRank() != null && !item.requiresRank().isBlank()) {
            String activeRank = grantService.activeRank(uuid);
            if (!item.requiresRank().equalsIgnoreCase(activeRank)) return false;
        }

        if (!creditService.spend(uuid, item.cost())) return false;

        switch (item.type()) {
            case RANK, RANK_UPGRADE -> grantService.grant(
                    uuid, player.getName(), item.value(),
                    GrantDuration.parse("permanent"),
                    CONSOLE_UUID, CONSOLE_NAME, "Credit Shop Purchase");
            case CRATE -> {
                CrateDefinition crate = crateService.getCrate(item.value());
                if (crate != null) {
                    crateService.rollFree(uuid, crate);
                }
            }
            case COSMETIC -> {
                CosmeticManager.getInstance().grantCosmetic(uuid, item.value());
                cosmeticService.persistGrant(uuid, item.value(), "CREDIT_SHOP", "creditshop");
            }
        }

        return true;
    }

    @NotNull
    public List<ShopCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }
}

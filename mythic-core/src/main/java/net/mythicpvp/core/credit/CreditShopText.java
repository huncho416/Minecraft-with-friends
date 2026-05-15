package net.mythicpvp.core.credit;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CreditShopText {

    @Nullable
    private final MythicConfig config;

    public CreditShopText(@Nullable MythicConfig config) {
        this.config = config;
    }

    @NotNull
    public String shopTitle() {
        return get("creditshop.shop-title", "&#FFD700Credit Shop");
    }

    @NotNull
    public String categoryTitle(@NotNull String categoryName) {
        return get("creditshop.category-title", "&#FFD700%category%").replace("%category%", categoryName);
    }

    @NotNull
    public String confirmTitle(@NotNull String itemName) {
        return get("creditshop.confirm-title", "&#FFD700Purchase %item%?").replace("%item%", itemName);
    }

    @NotNull
    public String clickToView() {
        return get("creditshop.click-to-view", "&#D2D8E0Click to browse");
    }

    @NotNull
    public String confirm() {
        return get("creditshop.confirm", "&#9CFF9CConfirm Purchase");
    }

    @NotNull
    public String cancel() {
        return get("creditshop.cancel", "&#FF8A8ACancel");
    }

    @NotNull
    private String get(@NotNull String path, @NotNull String def) {
        return config == null ? def : config.getString(path, def);
    }
}

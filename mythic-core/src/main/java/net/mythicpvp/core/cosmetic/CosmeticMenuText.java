package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CosmeticMenuText {

    public static final CosmeticMenuText DEFAULTS = new CosmeticMenuText(null);

    @Nullable
    private final MythicConfig config;

    public CosmeticMenuText(@Nullable MythicConfig config) {
        this.config = config;
    }

    @NotNull
    public String mainTitle() {
        return get("cosmetics.main-title", "&#F529BECosmetics");
    }

    @NotNull
    public String browseTitle(@NotNull String typeName) {
        return with(get("cosmetics.browse-title", "&#F529BE%type%"), "%type%", typeName);
    }

    @NotNull
    public String detailTitle(@NotNull String cosmeticName) {
        return with(get("cosmetics.detail-title", "&#F529BE%cosmetic%"), "%cosmetic%", cosmeticName);
    }

    @NotNull
    public String cratesTitle() {
        return get("cosmetics.crates-title", "&#F529BECrates");
    }

    @NotNull
    public String crateConfirmTitle(@NotNull String crateName) {
        return with(get("cosmetics.crate-confirm-title", "&#F529BEOpen %crate%?"), "%crate%", crateName);
    }

    @NotNull
    public String clickToView() {
        return get("cosmetics.click-to-view", "&#D2D8E0Click to view");
    }

    @NotNull
    public String clickToEquip() {
        return get("cosmetics.click-to-equip", "&#9CFF9CClick to equip");
    }

    @NotNull
    public String clickToUnequip() {
        return get("cosmetics.click-to-unequip", "&#FF8A8AClick to unequip");
    }

    @NotNull
    public String withdraw() {
        return get("cosmetics.withdraw", "&#F529BEWithdraw as Item");
    }

    @NotNull
    public String equipped() {
        return get("cosmetics.equipped", "&#9CFF9CEquipped");
    }

    @NotNull
    public String owned() {
        return get("cosmetics.owned", "&#9CFF9COwned");
    }

    @NotNull
    public String notOwned() {
        return get("cosmetics.not-owned", "&#FF8A8ANot Owned");
    }

    @NotNull
    public String confirm() {
        return get("cosmetics.confirm", "&#9CFF9CConfirm");
    }

    @NotNull
    public String cancel() {
        return get("cosmetics.cancel", "&#FF8A8ACancel");
    }

    @NotNull
    public String openCrates() {
        return get("cosmetics.open-crates", "&#F529BECrates");
    }

    @NotNull
    public String back() {
        return get("cosmetics.back", "&#F529BEBack");
    }

    @NotNull
    private String get(@NotNull String path, @NotNull String def) {
        return config == null ? def : config.getString(path, def);
    }

    @NotNull
    private static String with(@NotNull String template, @NotNull String token, @NotNull String value) {
        return template.replace(token, value);
    }
}

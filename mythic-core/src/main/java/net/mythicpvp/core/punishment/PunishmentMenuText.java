package net.mythicpvp.core.punishment;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PunishmentMenuText {

    public static final PunishmentMenuText DEFAULTS = new PunishmentMenuText(null);

    @Nullable
    private final MythicConfig config;

    public PunishmentMenuText(@Nullable MythicConfig config) {
        this.config = config;
    }

    @NotNull
    public String punishTitle(@NotNull String targetName) {
        return with(get("punishment.punish-title", "&#F529BEPunish: %target%"),
                "%target%", targetName);
    }

    @NotNull
    public String handbookTitle() {
        return get("punishment.handbook-title", "&#F529BEPunishments");
    }

    @NotNull
    public String templatesTitle(@NotNull String categoryName) {
        return with(get("punishment.templates-title", "&#F529BE%category%"),
                "%category%", categoryName);
    }

    @NotNull
    public String proofTitle() {
        return get("punishment.proof-title", "&#F529BEPunishment Proof");
    }

    @NotNull
    public String confirmTitle() {
        return get("punishment.confirm-title", "&#F529BEConfirm Punishment");
    }

    @NotNull
    public String historyTitle(@NotNull String targetName) {
        return with(get("punishment.history-title", "&#F529BEHistory: %target%"),
                "%target%", targetName);
    }

    @NotNull
    public String categoryName(@NotNull String categoryName) {
        return with(get("punishment.category-button.name", "&#F529BE%category%"),
                "%category%", categoryName);
    }

    @NotNull
    public String categoryLoreTemplates() {
        return get("punishment.category-button.lore-templates", "&7View templates");
    }

    @NotNull
    public String categoryLoreHandbook() {
        return get("punishment.category-button.lore-handbook", "&7Open handbook category");
    }

    @NotNull
    public String templateClickHint(boolean executable) {
        return executable
                ? get("punishment.template-click.executable", "&#F529BEClick to select")
                : get("punishment.template-click.handbook", "&#F529BERead-only handbook entry");
    }

    @NotNull
    public String enterProofName() {
        return get("punishment.buttons.enter-proof", "&#F529BEEnter Proof");
    }

    @NotNull
    public String enterProofLore() {
        return get("punishment.buttons.enter-proof-lore", "&7Click to enter proof in chat");
    }

    @NotNull
    public String proofSummaryName() {
        return get("punishment.buttons.proof-summary", "&#F529BEProof");
    }

    @NotNull
    public String noProofYet() {
        return get("punishment.buttons.no-proof-yet", "&7No proof entered");
    }

    @NotNull
    public String noProofButton() {
        return get("punishment.buttons.no-proof-button", "&#FF8A8ANo proof entered");
    }

    @NotNull
    public String confirmProof() {
        return get("punishment.buttons.confirm-proof", "&#9CFF9CConfirm Proof");
    }

    @NotNull
    public String clearInventoryName() {
        return get("punishment.buttons.clear-inventory", "&#F529BEClear Inventory");
    }

    @NotNull
    public String silentName() {
        return get("punishment.buttons.silent", "&#F529BESilent");
    }

    @NotNull
    public String summaryName() {
        return get("punishment.buttons.summary", "&#F529BEPunishment Summary");
    }

    @NotNull
    public String executeName() {
        return get("punishment.buttons.execute", "&#9CFF9CExecute Punishment");
    }

    @NotNull
    public String statePrefix() {
        return get("punishment.buttons.state-prefix", "&7State: &f");
    }

    @NotNull
    public String toggleHint() {
        return get("punishment.buttons.toggle-hint", "&#F529BEClick to toggle");
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

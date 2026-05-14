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
        return with(get("punishment.punish-title", "&#FF00F8Punish: %target%"),
                "%target%", targetName);
    }

    @NotNull
    public String handbookTitle() {
        return get("punishment.handbook-title", "&#FF00F8Punishments");
    }

    @NotNull
    public String templatesTitle(@NotNull String categoryName) {
        return with(get("punishment.templates-title", "&#FF00F8%category%"),
                "%category%", categoryName);
    }

    @NotNull
    public String proofTitle() {
        return get("punishment.proof-title", "&#FF00F8Punishment Proof");
    }

    @NotNull
    public String confirmTitle() {
        return get("punishment.confirm-title", "&#FF00F8Confirm Punishment");
    }

    @NotNull
    public String historyTitle(@NotNull String targetName) {
        return with(get("punishment.history-title", "&#FF00F8History: %target%"),
                "%target%", targetName);
    }

    @NotNull
    public String categoryName(@NotNull String categoryName) {
        return with(get("punishment.category-button.name", "&#FF00F8%category%"),
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
                ? get("punishment.template-click.executable", "&#FF00F8Click to select")
                : get("punishment.template-click.handbook", "&#FF00F8Read-only handbook entry");
    }

    @NotNull
    public String enterProofName() {
        return get("punishment.buttons.enter-proof", "&#FF00F8Enter Proof");
    }

    @NotNull
    public String enterProofLore() {
        return get("punishment.buttons.enter-proof-lore", "&7Click to enter proof in chat");
    }

    @NotNull
    public String proofSummaryName() {
        return get("punishment.buttons.proof-summary", "&#FF00F8Proof");
    }

    @NotNull
    public String noProofYet() {
        return get("punishment.buttons.no-proof-yet", "&7No proof entered");
    }

    @NotNull
    public String noProofButton() {
        return get("punishment.buttons.no-proof-button", "&#FF0000No proof entered");
    }

    @NotNull
    public String confirmProof() {
        return get("punishment.buttons.confirm-proof", "&#00FF00Confirm Proof");
    }

    @NotNull
    public String clearInventoryName() {
        return get("punishment.buttons.clear-inventory", "&#FF00F8Clear Inventory");
    }

    @NotNull
    public String silentName() {
        return get("punishment.buttons.silent", "&#FF00F8Silent");
    }

    @NotNull
    public String summaryName() {
        return get("punishment.buttons.summary", "&#FF00F8Punishment Summary");
    }

    @NotNull
    public String executeName() {
        return get("punishment.buttons.execute", "&#00FF00Execute Punishment");
    }

    @NotNull
    public String statePrefix() {
        return get("punishment.buttons.state-prefix", "&7State: &f");
    }

    @NotNull
    public String toggleHint() {
        return get("punishment.buttons.toggle-hint", "&#FF00F8Click to toggle");
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

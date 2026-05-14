package net.mythicpvp.core.rank;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Operator-overridable strings for the rank editor + /grant flow menus.
 *
 * <p>Loaded from {@code menus.yml} (the {@code rank.*} subtree). Mirrors
 * {@link net.mythicpvp.core.punishment.PunishmentMenuText}: every getter
 * has a hard-coded default that matches the historical UI, so missing
 * keys (or the whole file) keep the menus rendering.
 */
public final class RankMenuText {

    /** Singleton "use defaults everywhere" instance for tests / no-config callers. */
    public static final RankMenuText DEFAULTS = new RankMenuText(null);

    @Nullable
    private final MythicConfig config;

    public RankMenuText(@Nullable MythicConfig config) {
        this.config = config;
    }

    @NotNull
    public String grantRankTitle(@NotNull String targetName) {
        return with(get("rank.grant-rank-title", "&#FF00F8Grant: %target%"),
                "%target%", targetName);
    }

    @NotNull
    public String grantDurationTitle() {
        return get("rank.grant-duration-title", "&#FF00F8Grant Duration");
    }

    @NotNull
    public String grantReasonTitle() {
        return get("rank.grant-reason-title", "&#FF00F8Grant Reason");
    }

    @NotNull
    public String grantConfirmTitle() {
        return get("rank.grant-confirm-title", "&#FF00F8Confirm Grant");
    }

    @NotNull
    public String editorTitle(@NotNull String rankId) {
        return with(get("rank.editor-title", "&#FF00F8Rank Editor: %rank-id%"),
                "%rank-id%", rankId);
    }

    @NotNull
    public String clickToSelect() {
        return get("rank.click-to-select", "&#FF00F8Click to select");
    }

    @NotNull
    public String custom() {
        return get("rank.custom", "&#FF00F8Custom");
    }

    @NotNull
    public String summary() {
        return get("rank.summary", "&#FF00F8Grant Summary");
    }

    @NotNull
    public String confirm() {
        return get("rank.confirm", "&#00FF00Confirm");
    }

    @NotNull
    public String cancel() {
        return get("rank.cancel", "&#FF0000Cancel");
    }

    @NotNull
    public String editorDisplayFormats() {
        return get("rank.editor.display-formats", "&#FF00F8Display Formats");
    }

    @NotNull
    public String editorPermissions() {
        return get("rank.editor.permissions", "&#FF00F8Permissions");
    }

    @NotNull
    public String editorClose() {
        return get("rank.editor.close", "&#FF00F8Close");
    }

    @NotNull
    public String editorFieldsTitle() {
        return get("rank.editor.fields-title", "&#FF00F8Edit Fields");
    }

    @NotNull
    public String editorFormatsTitle() {
        return get("rank.editor.formats-title", "&#FF00F8Display Formats");
    }

    @NotNull
    public String editorPermissionsTitle(@NotNull String rankId) {
        return with(get("rank.editor.permissions-title", "&#FF00F8Permissions: %rank-id%"),
                "%rank-id%", rankId);
    }

    @NotNull
    public String editorAddPermission() {
        return get("rank.editor.add-permission", "&#00FF00Add Permission");
    }

    @NotNull
    public String editorAddPermissionLore() {
        return get("rank.editor.add-permission-lore", "&7Click and type the permission node in chat");
    }

    @NotNull
    public String editorRemovePermissionLore() {
        return get("rank.editor.remove-permission-lore", "&#FF0000Click to remove");
    }

    @NotNull
    public String editorFieldPrompt() {
        return get("rank.editor.field-name-prompt", "&7Type the new value in chat (or 'cancel').");
    }

    @NotNull
    public String editorFieldUpdated(@NotNull String field, @NotNull String value) {
        return with(with(get("rank.editor.field-updated", "&#00FF00Updated %field% to %value%"),
                "%field%", field), "%value%", value);
    }

    @NotNull
    public String editorFieldFailed(@NotNull String field) {
        return with(get("rank.editor.field-failed", "&#FF0000Failed to update %field%"),
                "%field%", field);
    }

    @NotNull
    public String editorBack() {
        return get("rank.editor.field-back", "&#FF00F8Back");
    }

    /**
     * Duration presets shown in the /grant duration menu. Empty list →
     * built-in defaults so the menu always has something to render.
     */
    @NotNull
    public List<String> durationPresets() {
        if (config == null) {
            return List.of("1d", "7d", "30d", "90d", "365d", "permanent");
        }
        List<String> raw = config.getStringList("rank.duration-presets");
        return raw.isEmpty() ? List.of("1d", "7d", "30d", "90d", "365d", "permanent") : raw;
    }

    /** Reason presets shown in the /grant reason menu. Empty → built-in defaults. */
    @NotNull
    public List<String> reasonPresets() {
        if (config == null) {
            return List.of("Staff Rank", "Rank Upgrade", "Purchased Rank");
        }
        List<String> raw = config.getStringList("rank.reason-presets");
        return raw.isEmpty() ? List.of("Staff Rank", "Rank Upgrade", "Purchased Rank") : raw;
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

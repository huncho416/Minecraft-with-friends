package net.mythicpvp.core.rank;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RankMenuText {

    public static final RankMenuText DEFAULTS = new RankMenuText(null);

    @Nullable
    private final MythicConfig config;

    public RankMenuText(@Nullable MythicConfig config) {
        this.config = config;
    }

    @NotNull
    public String grantRankTitle(@NotNull String targetName) {
        return with(get("rank.grant-rank-title", "&#F529BEGrant: %target%"),
                "%target%", targetName);
    }

    @NotNull
    public String grantDurationTitle() {
        return get("rank.grant-duration-title", "&#F529BEGrant Duration");
    }

    @NotNull
    public String grantReasonTitle() {
        return get("rank.grant-reason-title", "&#F529BEGrant Reason");
    }

    @NotNull
    public String grantConfirmTitle() {
        return get("rank.grant-confirm-title", "&#F529BEConfirm Grant");
    }

    @NotNull
    public String editorTitle(@NotNull String rankId) {
        return with(get("rank.editor-title", "&#F529BERank Editor: %rank-id%"),
                "%rank-id%", rankId);
    }

    @NotNull
    public String clickToSelect() {
        return get("rank.click-to-select", "&#F529BEClick to select");
    }

    @NotNull
    public String custom() {
        return get("rank.custom", "&#F529BECustom");
    }

    @NotNull
    public String summary() {
        return get("rank.summary", "&#F529BEGrant Summary");
    }

    @NotNull
    public String confirm() {
        return get("rank.confirm", "&#9CFF9CConfirm");
    }

    @NotNull
    public String cancel() {
        return get("rank.cancel", "&#FF8A8ACancel");
    }

    @NotNull
    public String editorDisplayFormats() {
        return get("rank.editor.display-formats", "&#F529BEDisplay Formats");
    }

    @NotNull
    public String editorPermissions() {
        return get("rank.editor.permissions", "&#F529BEPermissions");
    }

    @NotNull
    public String editorClose() {
        return get("rank.editor.close", "&#F529BEClose");
    }

    @NotNull
    public String editorFieldsTitle() {
        return get("rank.editor.fields-title", "&#F529BEEdit Fields");
    }

    @NotNull
    public String editorFormatsTitle() {
        return get("rank.editor.formats-title", "&#F529BEDisplay Formats");
    }

    @NotNull
    public String editorPermissionsTitle(@NotNull String rankId) {
        return with(get("rank.editor.permissions-title", "&#F529BEPermissions: %rank-id%"),
                "%rank-id%", rankId);
    }

    @NotNull
    public String editorAddPermission() {
        return get("rank.editor.add-permission", "&#9CFF9CAdd Permission");
    }

    @NotNull
    public String editorAddPermissionLore() {
        return get("rank.editor.add-permission-lore", "&7Click and type the permission node in chat");
    }

    @NotNull
    public String editorRemovePermissionLore() {
        return get("rank.editor.remove-permission-lore", "&#FF8A8AClick to remove");
    }

    @NotNull
    public String editorFieldPrompt() {
        return get("rank.editor.field-name-prompt", "&7Type the new value in chat (or 'cancel').");
    }

    @NotNull
    public String editorFieldUpdated(@NotNull String field, @NotNull String value) {
        return with(with(get("rank.editor.field-updated", "&#9CFF9CUpdated %field% to %value%"),
                "%field%", field), "%value%", value);
    }

    @NotNull
    public String editorFieldFailed(@NotNull String field) {
        return with(get("rank.editor.field-failed", "&#FF8A8AFailed to update %field%"),
                "%field%", field);
    }

    @NotNull
    public String editorBack() {
        return get("rank.editor.field-back", "&#F529BEBack");
    }

    @NotNull
    public List<String> durationPresets() {
        if (config == null) {
            return List.of("1d", "7d", "30d", "90d", "365d", "permanent");
        }
        List<String> raw = config.getStringList("rank.duration-presets");
        return raw.isEmpty() ? List.of("1d", "7d", "30d", "90d", "365d", "permanent") : raw;
    }

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

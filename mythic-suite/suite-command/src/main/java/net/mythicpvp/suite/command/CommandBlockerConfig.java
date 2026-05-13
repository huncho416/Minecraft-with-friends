package net.mythicpvp.suite.command;

import net.mythicpvp.suite.config.MythicConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CommandBlockerConfig {

    private final boolean enabled;
    private final boolean hideNoPermission;
    private final String bypassPermission;
    private final String deniedMessage;
    private final Set<String> blockedCommands;
    private final Map<String, String> requiredPermissions;

    CommandBlockerConfig(boolean enabled, boolean hideNoPermission, @NotNull String bypassPermission, @NotNull String deniedMessage, @NotNull Set<String> blockedCommands, @NotNull Map<String, String> requiredPermissions) {
        this.enabled = enabled;
        this.hideNoPermission = hideNoPermission;
        this.bypassPermission = bypassPermission;
        this.deniedMessage = deniedMessage;
        this.blockedCommands = Set.copyOf(blockedCommands);
        this.requiredPermissions = Map.copyOf(requiredPermissions);
    }

    @NotNull
    static CommandBlockerConfig load(@NotNull MythicConfig config) {
        boolean changed = false;
        if (!config.contains("enabled")) {
            config.set("enabled", true);
            changed = true;
        }
        if (!config.contains("hide-no-permission")) {
            config.set("hide-no-permission", true);
            changed = true;
        }
        if (!config.contains("bypass-permission")) {
            config.set("bypass-permission", "mythic.commandblocker.bypass");
            changed = true;
        }
        if (!config.contains("messages.blocked")) {
            config.set("messages.blocked", "&#FF00F8✘ &#FFFFFFUnknown command.");
            changed = true;
        }
        if (!config.contains("blocked-commands")) {
            config.set("blocked-commands", List.of("pl", "plugins", "bukkit:pl", "bukkit:plugins", "?", "help", "bukkit:?", "bukkit:help", "version", "ver", "bukkit:version", "bukkit:ver"));
            changed = true;
        }
        if (!config.contains("required-permissions")) {
            config.set("required-permissions.pl", "mythic.admin.commands.plugins");
            config.set("required-permissions.plugins", "mythic.admin.commands.plugins");
            config.set("required-permissions.bukkit:pl", "mythic.admin.commands.plugins");
            config.set("required-permissions.bukkit:plugins", "mythic.admin.commands.plugins");
            config.set("required-permissions.?", "mythic.admin.commands.help");
            config.set("required-permissions.help", "mythic.admin.commands.help");
            config.set("required-permissions.bukkit:?", "mythic.admin.commands.help");
            config.set("required-permissions.bukkit:help", "mythic.admin.commands.help");
            changed = true;
        }
        if (changed) {
            config.save();
        }
        Set<String> blocked = new LinkedHashSet<>();
        for (String command : config.getStringList("blocked-commands")) {
            String normalized = CommandBlocker.normalizeCommand(command);
            if (!normalized.isBlank()) {
                blocked.add(normalized);
            }
        }
        Map<String, String> permissions = new LinkedHashMap<>();
        if (config.getConfig().isConfigurationSection("required-permissions")) {
            for (String key : config.getConfig().getConfigurationSection("required-permissions").getKeys(false)) {
                String permission = config.getString("required-permissions." + key);
                String normalized = CommandBlocker.normalizeCommand(key);
                if (permission != null && !permission.isBlank() && !normalized.isBlank()) {
                    permissions.put(normalized, permission);
                }
            }
        }
        return new CommandBlockerConfig(
            config.getBoolean("enabled", true),
            config.getBoolean("hide-no-permission", true),
            config.getString("bypass-permission", "mythic.commandblocker.bypass"),
            config.getString("messages.blocked", "&#FF00F8✘ &#FFFFFFUnknown command."),
            blocked,
            permissions
        );
    }

    boolean enabled() {
        return enabled;
    }

    boolean hideNoPermission() {
        return hideNoPermission;
    }

    @NotNull
    String bypassPermission() {
        return bypassPermission;
    }

    @NotNull
    String deniedMessage() {
        return deniedMessage;
    }

    boolean isBlocked(@NotNull String command) {
        return blockedCommands.contains(command);
    }

    @Nullable
    String requiredPermission(@NotNull String command) {
        return requiredPermissions.get(command);
    }
}

package net.mythicpvp.suite.command;

import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CommandBlocker implements Listener {

    private final CommandManager commandManager;
    private final MythicConfig configFile;
    private CommandBlockerConfig config;

    CommandBlocker(@NotNull JavaPlugin plugin, @NotNull CommandManager commandManager) {
        this.commandManager = commandManager;
        this.configFile = new MythicConfig(plugin, "command-blocker.yml");
        reload();
    }

    CommandBlocker(@NotNull CommandManager commandManager, @NotNull CommandBlockerConfig config) {
        this.commandManager = commandManager;
        this.configFile = null;
        this.config = config;
    }

    public void reload() {
        if (configFile != null) {
            configFile.reload();
            config = CommandBlockerConfig.load(configFile);
        }
    }

    public boolean canUse(@NotNull CommandSender sender, @NotNull String commandLine) {
        if (config == null || !config.enabled()) {
            return true;
        }
        if (hasBypass(sender)) {
            return true;
        }
        String command = commandFromLine(commandLine);
        if (command.isBlank()) {
            return true;
        }
        String required = requiredPermission(command);
        if (required != null) {
            return sender.hasPermission(required);
        }
        return !config.isBlocked(command);
    }

    public boolean canSee(@NotNull CommandSender sender, @NotNull String command) {
        if (config == null || !config.enabled()) {
            return true;
        }
        if (hasBypass(sender)) {
            return true;
        }
        String normalized = normalizeCommand(command);
        if (normalized.isBlank()) {
            return true;
        }
        String required = requiredPermission(normalized);
        if (required != null) {
            return sender.hasPermission(required);
        }
        return !config.isBlocked(normalized);
    }

    @NotNull
    public List<String> filterCommands(@NotNull CommandSender sender, @NotNull Collection<String> commands) {
        List<String> filtered = new ArrayList<>();
        for (String command : commands) {
            if (canSee(sender, command)) {
                filtered.add(command);
            }
        }
        return filtered;
    }

    @NotNull
    public List<String> filterCompletions(@NotNull CommandSender sender, @NotNull String buffer, @NotNull Collection<String> completions) {
        if (!buffer.startsWith("/")) {
            return new ArrayList<>(completions);
        }
        if (!canUse(sender, buffer)) {
            return List.of();
        }
        String remaining = buffer.substring(1);
        if (!remaining.contains(" ")) {
            List<String> filtered = new ArrayList<>();
            for (String completion : completions) {
                if (canSee(sender, completion)) {
                    filtered.add(completion);
                }
            }
            return filtered;
        }
        return new ArrayList<>(completions);
    }

    public void sendDenied(@NotNull CommandSender sender) {
        if (config != null) {
            sender.sendMessage(MythicHex.colorize(config.deniedMessage()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandSend(@NotNull PlayerCommandSendEvent event) {
        event.getCommands().removeIf(command -> !canSee(event.getPlayer(), command));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
        if (!canUse(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            sendDenied(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTabComplete(@NotNull TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player) || !event.isCommand()) {
            return;
        }
        event.setCompletions(filterCompletions(player, event.getBuffer(), event.getCompletions()));
        if (event.getCompletions().isEmpty() && !canUse(player, event.getBuffer())) {
            event.setCancelled(true);
        }
    }

    @NotNull
    static String normalizeCommand(@NotNull String command) {
        String normalized = command.trim().toLowerCase();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int space = normalized.indexOf(' ');
        if (space >= 0) {
            normalized = normalized.substring(0, space);
        }
        return normalized;
    }

    @NotNull
    private static String commandFromLine(@NotNull String commandLine) {
        return normalizeCommand(commandLine);
    }

    private boolean hasBypass(@NotNull CommandSender sender) {
        return !config.bypassPermission().isBlank() && sender.hasPermission(config.bypassPermission());
    }

    private String requiredPermission(@NotNull String command) {
        String configured = config.requiredPermission(command);
        if (configured != null) {
            return configured;
        }
        if (config.hideNoPermission()) {
            return commandManager.getPermission(command);
        }
        return null;
    }
}

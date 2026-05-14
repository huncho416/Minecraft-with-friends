package net.mythicpvp.core.essentials;

import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CoreEssentialsService {

    private final CoreMessages messages;

    public CoreEssentialsService(@NotNull CoreMessages messages) {
        this.messages = messages;
    }

    public void setGameMode(@NotNull CommandSender sender, @NotNull String modeInput, @Nullable String targetName) {
        GameMode mode = parseGameMode(modeInput);
        if (mode == null) {
            sender.sendMessage(messages.component("essentials.invalid-gamemode", "&#FF00F8x &#FFFFFFUnknown gamemode."));
            return;
        }
        Player target = target(sender, targetName, "mythic.core.gamemode.others");
        if (target == null) {
            return;
        }
        target.setGameMode(mode);
        sender.sendMessage(messages.component("essentials.gamemode", "&#FF00F8+ &#FFFFFFSet %target%'s gamemode to %mode%.", Map.of(
                "target", target.getName(),
                "mode", display(mode)
        )));
        if (!sender.equals(target)) {
            target.sendMessage(messages.component("essentials.gamemode-received", "&#FF00F8+ &#FFFFFFYour gamemode was set to %mode%.", Map.of(
                    "target", target.getName(),
                    "mode", display(mode)
            )));
        }
    }

    public void teleport(@NotNull CommandSender sender, @NotNull String targetName, @Nullable String destinationName) {
        if (!(sender instanceof Player player) && (destinationName == null || destinationName.isBlank())) {
            sender.sendMessage(messages.component("command.usage", "&#FF00F8x &#FFFFFFUsage: %usage%", Map.of("usage", "/tp <player> <target>")));
            return;
        }
        Player destination;
        Player target;
        if (destinationName == null || destinationName.isBlank()) {
            target = (Player) sender;
            destination = online(targetName);
        } else {
            if (!sender.hasPermission("mythic.core.teleport.others")) {
                sender.sendMessage(messages.component("command.no-permission", "&#FF00F8x &#FFFFFFUnknown command."));
                return;
            }
            target = online(targetName);
            destination = online(destinationName);
        }
        if (target == null || destination == null) {
            sender.sendMessage(messages.component("command.player-not-found", "&#FF00F8x &#FFFFFFThat player is not online."));
            return;
        }
        Location location = destination.getLocation();
        target.teleport(location);
        sender.sendMessage(messages.component("essentials.teleport", "&#FF00F8+ &#FFFFFFTeleported %target% to %destination%.", Map.of(
                "target", target.getName(),
                "destination", destination.getName()
        )));
    }

    public void teleportHere(@NotNull CommandSender sender, @NotNull String targetName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.component("command.player-only", "&#FF00F8x &#FFFFFFThis command can only be used by players."));
            return;
        }
        Player target = online(targetName);
        if (target == null) {
            sender.sendMessage(messages.component("command.player-not-found", "&#FF00F8x &#FFFFFFThat player is not online."));
            return;
        }
        target.teleport(player.getLocation());
        sender.sendMessage(messages.component("essentials.tphere", "&#FF00F8+ &#FFFFFFTeleported %target% to you.", Map.of("target", target.getName())));
    }

    public void sendHelp(@NotNull CommandSender sender) {
        messages.list("links.help", List.of("&#FF00F8MythicPvP Help", "&#FFFFFFUse /discord for community support.")).forEach(sender::sendMessage);
    }

    public void sendDiscord(@NotNull CommandSender sender) {
        sender.sendMessage(messages.component("links.discord", "&#FF00F8Discord &8> &#FFFFFFdiscord.gg/mythicpvp"));
    }

    @Nullable
    public GameMode parseGameMode(@NotNull String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @Nullable
    private Player target(@NotNull CommandSender sender, @Nullable String targetName, @NotNull String othersPermission) {
        if (targetName == null || targetName.isBlank()) {
            if (sender instanceof Player player) {
                return player;
            }
            sender.sendMessage(messages.component("command.usage", "&#FF00F8x &#FFFFFFUsage: %usage%", Map.of("usage", "/gamemode <mode> <player>")));
            return null;
        }
        if (sender instanceof Player player && player.getName().equalsIgnoreCase(targetName)) {
            return player;
        }
        if (!sender.hasPermission(othersPermission)) {
            sender.sendMessage(messages.component("command.no-permission", "&#FF00F8x &#FFFFFFUnknown command."));
            return null;
        }
        Player target = online(targetName);
        if (target == null) {
            sender.sendMessage(messages.component("command.player-not-found", "&#FF00F8x &#FFFFFFThat player is not online."));
        }
        return target;
    }

    @Nullable
    private Player online(@NotNull String name) {
        return Bukkit.getPlayerExact(name);
    }

    @NotNull
    private String display(@NotNull GameMode mode) {
        String lower = mode.name().toLowerCase(Locale.ROOT);
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }
}

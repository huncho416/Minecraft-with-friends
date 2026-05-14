package net.mythicpvp.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CoreCompletionValues {

    private CoreCompletionValues() {}

    @NotNull
    public static List<String> onlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @NotNull
    public static List<String> gamemodes() {
        return List.of("creative", "survival", "adventure", "spectator");
    }

    @NotNull
    public static List<String> gamemodeTargets(@NotNull CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission("mythic.core.gamemode.others") ? onlinePlayers() : List.of();
    }

    @NotNull
    public static List<String> teleportOthers(@NotNull CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission("mythic.core.teleport.others") ? onlinePlayers() : List.of();
    }
}

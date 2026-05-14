package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.command.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CoreCompletions {

    private CoreCompletions() {}

    public static void register(@NotNull CommandManager commandManager, @NotNull RankService rankService) {
        commandManager.registerCompletion("players", context -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        commandManager.registerCompletion("ranks", context -> rankService.ids());
        commandManager.registerCompletion("grant-durations", context -> List.of("1d", "7d", "30d", "90d", "365d", "permanent"));
        commandManager.registerCompletion("grant-reasons", context -> List.of("Staff", "Rank", "Upgrade", "Purchased"));
        commandManager.registerCompletion("rank-fields", context -> List.of("name", "color", "dye", "prefix", "suffix", "weight", "staff", "donator", "parent", "chat-prefix", "chat-format", "tab-prefix", "tab-format", "nametag-prefix", "nametag-format"));
        commandManager.registerCompletion("booleans", context -> List.of("true", "false"));
    }
}

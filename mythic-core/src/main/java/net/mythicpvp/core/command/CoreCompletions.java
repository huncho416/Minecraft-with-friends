package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.transfer.ShardRegistry;
import net.mythicpvp.suite.command.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CoreCompletions {

    private CoreCompletions() {}

    public static void register(@NotNull CommandManager commandManager, @NotNull RankService rankService, @NotNull PunishmentService punishmentService, @NotNull ShardRegistry shardRegistry) {
        commandManager.registerCompletion("players", context -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        commandManager.registerCompletion("ranks", context -> rankService.ids());
        commandManager.registerCompletion("shards", context -> shardRegistry.shardIds());
        commandManager.registerCompletion("grant-durations", context -> List.of("1d", "7d", "30d", "90d", "365d", "permanent"));
        commandManager.registerCompletion("grant-reasons", context -> List.of("Staff", "Rank", "Upgrade", "Purchased"));
        commandManager.registerCompletion("rank-fields", context -> List.of("name", "color", "dye", "prefix", "suffix", "weight", "staff", "donator", "parent", "chat-prefix", "chat-format", "tab-prefix", "tab-format", "nametag-prefix", "nametag-format"));
        commandManager.registerCompletion("booleans", context -> List.of("true", "false"));
        commandManager.registerCompletion("punishment-categories", context -> java.util.Arrays.stream(PunishmentCategory.values()).map(Enum::name).toList());
        commandManager.registerCompletion("punishment-durations", context -> List.of("1d", "3d", "7d", "30d", "90d", "365d", "permanent"));
        commandManager.registerCompletion("punishment-templates", context -> punishmentService.templates().stream().map(template -> template.title()).toList());
        commandManager.registerCompletion("gamemodes", context -> CoreCompletionValues.gamemodes());
        commandManager.registerCompletion("essentials-targets", context -> CoreCompletionValues.gamemodeTargets(context.sender()));
        commandManager.registerCompletion("teleport-others", context -> CoreCompletionValues.teleportOthers(context.sender()));
        commandManager.registerCompletion("chat-scopes", context -> List.of("local", "network"));
        commandManager.registerCompletion("chat-slow-presets", context -> List.of("0", "3", "5", "10", "30"));
    }
}

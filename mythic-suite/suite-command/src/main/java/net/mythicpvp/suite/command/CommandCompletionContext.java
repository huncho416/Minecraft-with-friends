package net.mythicpvp.suite.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public record CommandCompletionContext(
        @NotNull CommandSender sender,
        @NotNull String alias,
        @NotNull String subcommand,
        @NotNull String[] args
) {
    @NotNull
    public String current() {
        return args.length == 0 ? "" : args[args.length - 1];
    }

    @NotNull
    public List<String> arguments() {
        return List.copyOf(Arrays.asList(args));
    }
}

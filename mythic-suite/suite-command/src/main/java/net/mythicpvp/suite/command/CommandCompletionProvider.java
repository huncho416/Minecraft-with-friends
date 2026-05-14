package net.mythicpvp.suite.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface CommandCompletionProvider {
    @NotNull
    List<String> complete(@NotNull CommandCompletionContext context);
}

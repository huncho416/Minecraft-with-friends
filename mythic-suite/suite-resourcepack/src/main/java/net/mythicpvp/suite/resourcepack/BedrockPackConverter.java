package net.mythicpvp.suite.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface BedrockPackConverter {
    @NotNull
    CompletableFuture<Path> convert(@NotNull Path javaPack);
}

package net.mythicpvp.suite.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class CommandBedrockPackConverter implements BedrockPackConverter {

    private final List<String> command;
    private final Path outputDirectory;
    private final Duration timeout;

    public CommandBedrockPackConverter(@NotNull List<String> command, @NotNull Path outputDirectory) {
        this(command, outputDirectory, Duration.ofMinutes(2));
    }

    public CommandBedrockPackConverter(@NotNull List<String> command, @NotNull Path outputDirectory, @NotNull Duration timeout) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }
        this.command = List.copyOf(command);
        this.outputDirectory = outputDirectory;
        this.timeout = timeout;
    }

    @Override
    @NotNull
    public CompletableFuture<Path> convert(@NotNull Path javaPack) {
        return CompletableFuture.supplyAsync(() -> convertBlocking(javaPack));
    }

    @NotNull
    private Path convertBlocking(@NotNull Path javaPack) {
        try {
            Files.createDirectories(outputDirectory);
            Path output = outputDirectory.resolve(outputName(javaPack));
            Process process = new ProcessBuilder(resolveCommand(javaPack, output))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Bedrock pack conversion timed out");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Bedrock pack conversion failed with exit code " + process.exitValue());
            }
            if (!Files.isRegularFile(output)) {
                throw new IllegalStateException("Bedrock pack converter did not create " + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to run Bedrock pack converter", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bedrock pack conversion interrupted", e);
        }
    }

    @NotNull
    private List<String> resolveCommand(@NotNull Path input, @NotNull Path output) {
        List<String> resolved = new ArrayList<>(command.size());
        for (String part : command) {
            resolved.add(part
                    .replace("{input}", input.toAbsolutePath().toString())
                    .replace("{output}", output.toAbsolutePath().toString()));
        }
        return resolved;
    }

    @NotNull
    private String outputName(@NotNull Path javaPack) {
        String fileName = javaPack.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base + ".mcpack";
    }
}

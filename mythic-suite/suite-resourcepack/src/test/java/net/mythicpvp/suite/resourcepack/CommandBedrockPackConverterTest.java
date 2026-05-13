package net.mythicpvp.suite.resourcepack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBedrockPackConverterTest {

    @Test
    void createsMcpackThroughConfiguredCommand() throws Exception {
        Path workingDirectory = Files.createTempDirectory("mythic-pack-test");
        Path input = workingDirectory.resolve("mythic-java-pack.zip");
        Path outputDirectory = workingDirectory.resolve("bedrock");
        Files.writeString(input, "pack");
        String java = Path.of(System.getProperty("java.home"), "bin", executable()).toString();
        CommandBedrockPackConverter converter = new CommandBedrockPackConverter(List.of(
                java,
                "-cp",
                System.getProperty("java.class.path"),
                TestPackCopier.class.getName(),
                "{input}",
                "{output}"
        ), outputDirectory, Duration.ofSeconds(10));
        Path converted = converter.convert(input).join();
        assertEquals("mythic-java-pack.mcpack", converted.getFileName().toString());
        assertTrue(Files.isRegularFile(converted));
        assertEquals("pack", Files.readString(converted));
    }

    private static String executable() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
    }
}

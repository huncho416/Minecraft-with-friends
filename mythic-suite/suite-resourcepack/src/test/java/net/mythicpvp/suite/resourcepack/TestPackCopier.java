package net.mythicpvp.suite.resourcepack;

import java.nio.file.Files;
import java.nio.file.Path;

final class TestPackCopier {

    private TestPackCopier() {}

    public static void main(String[] args) throws Exception {
        Files.copy(Path.of(args[0]), Path.of(args[1]));
    }
}

package net.mythicpvp.suite.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandAnnotationTest {

    @Test
    void commandAnnotationsAreRuntimeVisible() throws Exception {
        assertEquals("test|t", TestCommand.class.getAnnotation(CommandAlias.class).value());
        assertEquals("mythic.test", TestCommand.class.getAnnotation(CommandPermission.class).value());
        assertTrue(TestCommand.class.getDeclaredMethod("defaultCommand").isAnnotationPresent(Default.class));
        assertEquals("reload", TestCommand.class.getDeclaredMethod("reload").getAnnotation(Subcommand.class).value());
    }

    @CommandAlias("test|t")
    @CommandPermission("mythic.test")
    static final class TestCommand extends MythicCommand {
        @Default
        public void defaultCommand() {}

        @Subcommand("reload")
        public void reload() {}
    }
}

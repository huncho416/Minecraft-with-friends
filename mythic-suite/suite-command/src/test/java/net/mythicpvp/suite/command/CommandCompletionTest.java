package net.mythicpvp.suite.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandCompletionTest {

    private ServerMock server;

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void usesRegisteredCompletionProviderForDefaultCommandArguments() {
        server = MockBukkit.mock();
        JavaPlugin plugin = MockBukkit.createMockPlugin();
        CommandManager manager = new CommandManager(plugin);
        manager.registerCompletion("ranks", context -> List.of("default", "owner", "admin"));
        manager.register(new CompletionCommand());
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "mythic.complete", true);

        assertEquals(List.of("owner"), manager.tabComplete(player, "complete", new String[]{"o"}));
    }

    @CommandAlias("complete")
    @CommandPermission("mythic.complete")
    static final class CompletionCommand extends MythicCommand {
        @Default
        @Complete({"ranks"})
        public void execute() {}
    }
}

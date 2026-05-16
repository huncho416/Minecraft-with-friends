package net.mythicpvp.core.command;

import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("request")
public final class RequestCommand extends MythicCommand {

    private static final String COOLDOWN_KEY = "helpop";

    private final ReportConfig config;
    private final String localShardId;

    public RequestCommand(@NotNull ReportConfig config, @NotNull String localShardId) {
        this.config = config;
        this.localShardId = localShardId;
    }

    @Default
    public void execute(@NotNull Player player, String[] words) {
        HelpopCommand.HelpopSupport.execute(player, words, config, localShardId, COOLDOWN_KEY, "/request");
    }
}

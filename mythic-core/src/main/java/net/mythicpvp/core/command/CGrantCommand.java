package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.GrantDuration;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankGrant;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@CommandAlias("cgrant")
@CommandPermission("mythic.core.grant.command")
public final class CGrantCommand extends MythicCommand {

    private static final UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("console".getBytes(StandardCharsets.UTF_8));
    private final GrantService grantService;

    public CGrantCommand(@NotNull GrantService grantService) {
        this.grantService = grantService;
    }

    @Default
    @Complete({"players", "ranks", "grant-durations", "grant-reasons"})
    public void execute(@NotNull CommandSender sender, @NotNull String targetName, @NotNull String rankId, @NotNull String durationInput, @NotNull String[] reasonParts) {
        String reason = reasonParts.length == 0 ? "No reason specified" : String.join(" ", reasonParts);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID executorUuid = sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : CONSOLE_UUID;
        RankGrant grant = grantService.grant(target.getUniqueId(), targetName, rankId, GrantDuration.parse(durationInput), executorUuid, sender.getName(), reason);
        sender.sendMessage("Granted " + grant.rankId() + " to " + targetName + ".");
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.report.StaffNotifier;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.cooldown.CooldownManager;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@CommandAlias("helpop")
public final class HelpopCommand extends MythicCommand {

    private static final String COOLDOWN_KEY = "helpop";

    private final ReportConfig config;
    private final String localShardId;

    public HelpopCommand(@NotNull ReportConfig config, @NotNull String localShardId) {
        this.config = config;
        this.localShardId = localShardId;
    }

    @Default
    public void execute(@NotNull Player player, String[] words) {
        HelpopSupport.execute(player, words, config, localShardId, COOLDOWN_KEY, "/helpop");
    }

    static final class HelpopSupport {
        private HelpopSupport() {
        }

        static void execute(@NotNull Player player,
                            String[] words,
                            @NotNull ReportConfig config,
                            @NotNull String localShardId,
                            @NotNull String cooldownKey,
                            @NotNull String label) {
            if (words == null || words.length == 0) {
                sendUsage(player, config, label);
                return;
            }
            String message = String.join(" ", words).trim();
            if (message.isEmpty()) {
                sendUsage(player, config, label);
                return;
            }
            CooldownManager cd = CooldownManager.getInstance();
            if (cd.isOnCooldown(player.getUniqueId(), cooldownKey)) {
                long secs = (cd.getRemainingMillis(player.getUniqueId(), cooldownKey) + 999) / 1000;
                player.sendMessage(MythicHex.colorize(
                        "&#FF8A8APlease wait &#FFFFFF" + secs
                                + "s&#FF8A8A before sending another helpop request."));
                return;
            }
            cd.set(player.getUniqueId(), cooldownKey, config.helpopCooldownSeconds(), TimeUnit.SECONDS);
            StaffNotifier.notifyHelpop(player, localShardId, message);
            player.sendMessage(MythicHex.colorize(
                    "&#9CFF9CHelpop request sent. Online staff have been notified."));
        }

        static void sendUsage(@NotNull Player player, @NotNull ReportConfig config, @NotNull String label) {
            player.sendMessage(MythicHex.colorize("&#F529BEHelpop &7System"));
            player.sendMessage(MythicHex.colorize("&7Use &#FFFFFF" + label + " <message>&7 to ask staff for help."));
            player.sendMessage(MythicHex.colorize("&7Your message goes to all online staff with your name and server."));
            player.sendMessage(MythicHex.colorize("&7Cooldown: &f" + config.helpopCooldownSeconds() + "s &7per request."));
            player.sendMessage(MythicHex.colorize("&7Abuse will result in punishment."));
        }
    }
}

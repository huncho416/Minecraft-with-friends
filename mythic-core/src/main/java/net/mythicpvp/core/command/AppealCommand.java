package net.mythicpvp.core.command;

import net.mythicpvp.core.audit.CoreAuditLog;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.persistence.PersistenceGateway;
import net.mythicpvp.core.punishment.PunishmentRecord;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@CommandAlias("appeal")
public final class AppealCommand extends MythicCommand {

    private final PunishmentService punishments;
    private final PersistenceGateway persistence;
    private final CoreMessages messages;
    private final CoreAuditLog audit;

    public AppealCommand(
            @NotNull PunishmentService punishments,
            @NotNull PersistenceGateway persistence,
            @NotNull CoreMessages messages,
            @NotNull CoreAuditLog audit) {
        this.punishments = punishments;
        this.persistence = persistence;
        this.messages = messages;
        this.audit = audit;
    }

    @Default
    public void execute(@NotNull Player player, @NotNull String[] words) {
        if (words.length == 0) {
            player.sendMessage(messages.component(
                    "messages.punishment.appeal-usage",
                    "&#FF00F8✘ &#FFFFFFUsage: /appeal <message…>"));
            return;
        }

        PunishmentRecord target = punishments.active(player.getUniqueId()).stream()
                .max(java.util.Comparator.comparingLong(PunishmentRecord::createdAtMillis))
                .orElse(null);
        if (target == null) {
            player.sendMessage(messages.component(
                    "messages.punishment.appeal-none",
                    "&#FF00F8✘ &#FFFFFFYou have no active punishment to appeal."));
            return;
        }
        String message = String.join(" ", words);
        persistence.appealOpen(target.id(), player.getUniqueId(), message);
        audit.log("APPEAL_OPEN",
                player.getUniqueId(), player.getName(),
                player.getUniqueId(), player.getName(),
                Map.of(
                        "punishment_id", Long.toString(target.id()),
                        "kind", target.type().name()));
        player.sendMessage(messages.component(
                "messages.punishment.appeal-filed",
                "&#FF00F8Appeal &8» &#FFFFFFYour appeal for punishment #%id% has been filed.",
                Map.of("id", Long.toString(target.id()))));
    }
}

package net.mythicpvp.core.command;

import net.mythicpvp.core.audit.CoreAuditLog;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.persistence.PersistenceGateway;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Optional;
import net.mythicpvp.suite.command.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@CommandAlias("appeals")
@CommandPermission("mythic.core.punish.appeal")
public final class AppealsCommand extends MythicCommand {

    private final PersistenceGateway persistence;
    private final CoreMessages messages;
    private final CoreAuditLog audit;

    public AppealsCommand(
            @NotNull PersistenceGateway persistence,
            @NotNull CoreMessages messages,
            @NotNull CoreAuditLog audit) {
        this.persistence = persistence;
        this.messages = messages;
        this.audit = audit;
    }

    @Default
    public void usage(@NotNull CommandSender sender) {
        sender.sendMessage(messages.component(
                "messages.punishment.appeals-usage",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8AUsage: &#FFFFFF/appeals review <approve|deny> <appealId> [notes…]"));
    }

    @Subcommand("review")
    public void review(
            @NotNull CommandSender sender,
            @NotNull String decision,
            long appealId,
            @Optional String[] notesArr) {
        String normalized = decision.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("APPROVE") && !normalized.equals("APPROVED")
                && !normalized.equals("DENY") && !normalized.equals("DENIED")) {
            usage(sender);
            return;
        }
        String wireDecision = (normalized.startsWith("APP")) ? "APPROVED" : "DENIED";
        String notes = notesArr == null ? "" : String.join(" ", notesArr);

        UUID reviewerUuid;
        String reviewerName;
        if (sender instanceof Player p) {
            reviewerUuid = p.getUniqueId();
            reviewerName = p.getName();
        } else {

            reviewerUuid = net.mythicpvp.core.punishment.PunishmentService.SYSTEM_STAFF;
            reviewerName = "Console";
        }

        persistence.appealReview(appealId, reviewerUuid, wireDecision, notes);
        audit.log("APPEAL_REVIEW",
                reviewerUuid, reviewerName,
                null, "-",
                Map.of(
                        "appeal_id", Long.toString(appealId),
                        "decision", wireDecision,
                        "notes", notes.isEmpty() ? "-" : notes));
        sender.sendMessage(messages.component(
                "messages.punishment.appeal-reviewed",
                "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FFFFFFAppeal #%id% %decision%.",
                Map.of(
                        "id", Long.toString(appealId),
                        "decision", wireDecision.toLowerCase(Locale.ROOT))));
    }
}

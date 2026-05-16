package net.mythicpvp.core.command;

import net.mythicpvp.core.punishment.PunishmentCommand;
import net.mythicpvp.core.punishment.PunishmentRequest;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.punishment.PunishmentType;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

public abstract class PunishmentDirectCommand extends MythicCommand {

    private final PunishmentService punishments;
    private final PunishmentType type;
    private final boolean durationRequired;
    private final String serverId;
    private final Clock clock;

    protected PunishmentDirectCommand(@NotNull PunishmentService punishments,
                                      @NotNull PunishmentType type,
                                      boolean durationRequired,
                                      @NotNull String serverId,
                                      @NotNull Clock clock) {
        this.punishments = punishments;
        this.type = type;
        this.durationRequired = durationRequired;
        this.serverId = serverId;
        this.clock = clock;
    }

    protected final void run(@NotNull CommandSender sender, @NotNull String[] args) {
        try {
            PunishmentCommand parsed = PunishmentCommand.parse(args, durationRequired, "No reason specified");
            OfflinePlayer target = Bukkit.getOfflinePlayer(parsed.targetName());
            UUID staffUuid = sender instanceof Player player ? player.getUniqueId() : PunishmentService.SYSTEM_STAFF;
            var record = punishments.punish(new PunishmentRequest(
                    target.getUniqueId(),
                    parsed.targetName(),
                    staffUuid,
                    sender.getName(),
                    type,
                    parsed.reason(),
                    "",
                    parsed.expiresAt(clock.instant()),
                    parsed.silent(),
                    false,
                    serverId));
            sender.sendMessage(MythicHex.colorize("&#9CFF9CPunishment executed: &f" + record.type().name() + " " + record.targetName() + "&#9CFF9C."));
        } catch (RuntimeException failure) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUsage: " + usage()));
        }
    }

    @NotNull
    private String usage() {
        String duration = durationRequired ? " <duration>" : "";
        return "/" + type.command() + " <player>" + duration + " [reason] [-s]";
    }

    @CommandAlias("ban")
    @CommandPermission("mythic.core.punish.ban")
    public static final class Ban extends PunishmentDirectCommand {
        public Ban(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.BAN, false, s, c);
        }
        @Default
        @Complete({"players", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("tempban")
    @CommandPermission("mythic.core.punish.ban")
    public static final class TempBan extends PunishmentDirectCommand {
        public TempBan(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.TEMP_BAN, true, s, c);
        }
        @Default
        @Complete({"players", "punishment-durations", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("mute")
    @CommandPermission("mythic.core.punish.mute")
    public static final class Mute extends PunishmentDirectCommand {
        public Mute(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.MUTE, false, s, c);
        }
        @Default
        @Complete({"players", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("tempmute")
    @CommandPermission("mythic.core.punish.mute")
    public static final class TempMute extends PunishmentDirectCommand {
        public TempMute(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.TEMP_MUTE, true, s, c);
        }
        @Default
        @Complete({"players", "punishment-durations", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("blacklist")
    @CommandPermission("mythic.core.punish.blacklist")
    public static final class Blacklist extends PunishmentDirectCommand {
        public Blacklist(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.BLACKLIST, false, s, c);
        }
        @Default
        @Complete({"players", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("warn")
    @CommandPermission("mythic.core.punish.warn")
    public static final class Warn extends PunishmentDirectCommand {
        public Warn(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.WARN, false, s, c);
        }
        @Default
        @Complete({"players", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("kick")
    @CommandPermission("mythic.core.punish.kick")
    public static final class Kick extends PunishmentDirectCommand {
        public Kick(@NotNull PunishmentService p, @NotNull String s, @NotNull Clock c) {
            super(p, PunishmentType.KICK, false, s, c);
        }
        @Default
        @Complete({"players", "punishment-reasons"})
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) { run(sender, args); }
    }

    @CommandAlias("unban|pardon")
    @CommandPermission("mythic.core.punish.unban")
    public static final class Unban extends MythicCommand {
        private final PunishmentService punishments;

        public Unban(@NotNull PunishmentService punishments) {
            this.punishments = punishments;
        }

        @Default
        @Complete({"players"})
        public void execute(@NotNull CommandSender sender, @NotNull String targetName) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID staffUuid = sender instanceof Player player ? player.getUniqueId() : PunishmentService.SYSTEM_STAFF;
            int count = punishments.pardonActive(target.getUniqueId(),
                    Set.of(PunishmentType.BAN, PunishmentType.TEMP_BAN, PunishmentType.BLACKLIST),
                    staffUuid,
                    "Unbanned");
            sender.sendMessage(MythicHex.colorize(count > 0
                    ? "&#9CFF9CUnbanned &f" + targetName + "&#9CFF9C."
                    : "&#FF8A8ANo active ban found for &f" + targetName + "&#FF8A8A."));
        }
    }
}

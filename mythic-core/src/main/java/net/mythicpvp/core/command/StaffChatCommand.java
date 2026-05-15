package net.mythicpvp.core.command;

import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.staff.StaffChannel;
import net.mythicpvp.core.staff.StaffChannelService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class StaffChatCommand extends MythicCommand {

    private final StaffChannel channel;
    private final StaffChannelService staffChat;
    private final RankService ranks;
    private final GrantService grants;

    protected StaffChatCommand(
            @NotNull StaffChannel channel,
            @NotNull StaffChannelService staffChat,
            @NotNull RankService ranks,
            @NotNull GrantService grants) {
        this.channel = channel;
        this.staffChat = staffChat;
        this.ranks = ranks;
        this.grants = grants;
    }

    @Default
    public void execute(@NotNull Player sender, @NotNull String[] words) {
        if (words.length == 0) {
            boolean enabled = staffChat.toggle(sender.getUniqueId(), channel);
            sender.sendMessage(enabled
                    ? "Staff chat toggled: " + channel.id()
                    : "Staff chat toggled off.");
            return;
        }
        String message = String.join(" ", words);
        UUID uuid = sender.getUniqueId();

        String rankId = grants.activeRank(uuid);
        var rank = ranks.get(rankId);
        String rankName = rank == null ? "" : rank.name();
        String rankColor = rank == null ? "&7" : rank.color();
        staffChat.send(channel, uuid, sender.getName(), rankName, rankColor, message);
    }

    @CommandAlias("staffchat|sc")
    @CommandPermission("mythic.core.staffchat")
    public static final class Staff extends StaffChatCommand {
        public Staff(@NotNull StaffChannelService s, @NotNull RankService r, @NotNull GrantService g) {
            super(StaffChannel.STAFF, s, r, g);
        }
    }

    @CommandAlias("builderchat|bc")
    @CommandPermission("mythic.core.builderchat")
    public static final class Builder extends StaffChatCommand {
        public Builder(@NotNull StaffChannelService s, @NotNull RankService r, @NotNull GrantService g) {
            super(StaffChannel.BUILDER, s, r, g);
        }
    }

    @CommandAlias("managementchat|mc")
    @CommandPermission("mythic.core.managementchat")
    public static final class Management extends StaffChatCommand {
        public Management(@NotNull StaffChannelService s, @NotNull RankService r, @NotNull GrantService g) {
            super(StaffChannel.MANAGEMENT, s, r, g);
        }
    }

    @CommandAlias("adminchat|ac")
    @CommandPermission("mythic.core.adminchat")
    public static final class Admin extends StaffChatCommand {
        public Admin(@NotNull StaffChannelService s, @NotNull RankService r, @NotNull GrantService g) {
            super(StaffChannel.ADMIN, s, r, g);
        }
    }

    @CommandAlias("ownerchat|oc")
    @CommandPermission("mythic.core.ownerchat")
    public static final class Owner extends StaffChatCommand {
        public Owner(@NotNull StaffChannelService s, @NotNull RankService r, @NotNull GrantService g) {
            super(StaffChannel.OWNER, s, r, g);
        }
    }
}

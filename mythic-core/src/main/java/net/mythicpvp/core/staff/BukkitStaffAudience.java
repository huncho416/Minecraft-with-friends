package net.mythicpvp.core.staff;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {@link StaffAudience} implementation that renders inbound staff
 * messages to every online player who holds the channel's permission,
 * plus the console (so ops always see network-wide staff traffic).
 *
 * <p>Uses the {@code messages.staff.format} template from
 * {@code messages.yml} for placeholder substitution. Supported tokens
 * mirror what {@link StaffMessage} carries: {@code %server%},
 * {@code %sender%}, {@code %rank%}, {@code %rank_color%},
 * {@code %message%}, {@code %channel%}.
 */
public final class BukkitStaffAudience implements StaffAudience {

    private final String formatTemplate;

    public BukkitStaffAudience(@NotNull String formatTemplate) {
        this.formatTemplate = formatTemplate;
    }

    @Override
    public void accept(@NotNull StaffMessage message) {
        String rendered = interpolate(formatTemplate, Map.of(
                "server", message.server(),
                "sender", message.senderName(),
                "rank", message.rank(),
                "rank_color", message.rankColor(),
                "message", message.message(),
                "channel", message.channel().id()));
        Component component = MythicHex.colorize(rendered);

        // Render to every online player with the channel's permission.
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(message.channel().permission())) {
                viewer.sendMessage(component);
            }
        }
        // And to console so ops can see all staff chats from rcon.
        CommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(component);
    }

    @NotNull
    private static String interpolate(@NotNull String template, @NotNull Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}

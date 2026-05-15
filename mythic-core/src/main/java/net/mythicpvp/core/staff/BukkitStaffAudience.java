package net.mythicpvp.core.staff;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class BukkitStaffAudience implements StaffAudience {

    private final String formatTemplate;

    public BukkitStaffAudience(@NotNull String formatTemplate) {
        this.formatTemplate = formatTemplate;
    }

    @Override
    public void accept(@NotNull StaffMessage message) {
        StaffChannel channel = message.channel();
        String channelTag = channel.tagColor() + "[" + channel.tag() + "]";
        String rendered = interpolate(formatTemplate, Map.of(
                "server", message.server(),
                "sender", message.senderName(),
                "rank", message.rank(),
                "rank_color", coerceColor(message.rankColor()),
                "chat_prefix", message.chatPrefix(),
                "channel_tag", channelTag,
                "message", message.message(),
                "channel", channel.id()));
        Component component = MythicHex.colorize(rendered);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(channel.permission())) {
                viewer.sendMessage(component);
            }
        }

        CommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(component);
    }

    @NotNull
    private static String coerceColor(@NotNull String raw) {
        if (raw.isBlank()) {
            return "&7";
        }
        if (raw.startsWith("#") && !raw.startsWith("&#")) {
            return "&" + raw;
        }
        return raw;
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

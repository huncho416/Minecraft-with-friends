package net.mythicpvp.core.staff;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Renders {@link StaffPresenceEvent}s to permitted players + console.
 *
 * <p>Three templates pulled from {@code messages.staff.{join,quit,switch}}:
 * <ul>
 *   <li>JOIN  — {@code "&#888888[%server%] %rank_color%%sender% &#FFFFFFjoined."}
 *   <li>QUIT  — {@code "&#888888[%server%] %rank_color%%sender% &#FFFFFFdisconnected."}
 *   <li>SWITCH— {@code "&#888888[%from% -> %to%] %rank_color%%sender% &#FFFFFFswitched servers."}
 * </ul>
 *
 * <p>Visibility gated by {@link StaffPresenceListener#STAFF_PERMISSION}
 * — only staff see staff comings and goings, both for security and to
 * avoid spamming regular players.
 */
public final class BukkitStaffPresenceAudience implements Consumer<StaffPresenceEvent> {

    private final ConfigText messages;

    public BukkitStaffPresenceAudience(@NotNull ConfigText messages) {
        this.messages = messages;
    }

    @Override
    public void accept(@NotNull StaffPresenceEvent event) {
        String template = templateFor(event.type());
        String rendered = interpolate(template, Map.of(
                "server", event.server(),
                "sender", event.staffName(),
                "rank", event.rank(),
                "rank_color", event.rankColor(),
                "from", event.fromServer(),
                "to", event.toServer()));
        Component component = MythicHex.colorize(rendered);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission(StaffPresenceListener.STAFF_PERMISSION)) {
                viewer.sendMessage(component);
            }
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    @NotNull
    private String templateFor(@NotNull StaffPresenceType type) {
        return switch (type) {
            case JOIN -> messages.raw("messages.staff.join",
                    "&#888888[%server%] %rank_color%%sender% &#FFFFFFjoined.");
            case QUIT -> messages.raw("messages.staff.quit",
                    "&#888888[%server%] %rank_color%%sender% &#FFFFFFdisconnected.");
            case SWITCH -> messages.raw("messages.staff.switch",
                    "&#888888[%from% -> %to%] %rank_color%%sender% &#FFFFFFswitched servers.");
        };
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

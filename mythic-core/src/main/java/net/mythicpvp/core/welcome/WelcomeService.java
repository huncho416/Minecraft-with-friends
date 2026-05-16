package net.mythicpvp.core.welcome;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WelcomeService {

    private static final int CHAT_WIDTH_CHARS = 53;

    private final MythicConfig config;

    public WelcomeService(@NotNull MythicConfig config) {
        this.config = config;
    }

    public boolean enabled() {
        return config.getBoolean("welcome.enabled", true);
    }

    @NotNull
    public List<Component> render(@NotNull Player player) {
        List<Component> out = new ArrayList<>();
        if (!enabled()) {
            return out;
        }
        out.add(Component.empty());
        out.add(Component.empty());
        for (String line : config.getStringList("welcome.lines")) {
            String resolved = line.replace("%player%", player.getName());
            out.add(centered(resolved));
        }
        ConfigurationSection root = config.getConfig().getConfigurationSection("welcome");
        if (root != null) {
            List<?> linkList = root.getList("links");
            if (linkList != null && !linkList.isEmpty()) {
                out.add(Component.empty());
                for (Object obj : linkList) {
                    if (!(obj instanceof java.util.Map<?, ?> raw)) continue;
                    Object label = raw.get("label");
                    Object url = raw.get("url");
                    if (label == null || url == null) continue;
                    String labelText = String.valueOf(label).replace("%player%", player.getName());
                    Component link = centered(labelText)
                            .clickEvent(ClickEvent.openUrl(String.valueOf(url)));
                    Object hover = raw.get("hover");
                    if (hover != null) {
                        link = link.hoverEvent(HoverEvent.showText(MythicHex.colorize(String.valueOf(hover))));
                    }
                    out.add(link);
                }
            }
        }
        out.add(Component.empty());
        out.add(Component.empty());
        return out;
    }

    public void send(@NotNull Player player) {
        for (Component line : render(player)) {
            player.sendMessage(line);
        }
    }

    @NotNull
    private static Component centered(@NotNull String line) {
        Component component = MythicHex.colorize(line);
        String visible = PlainTextComponentSerializer.plainText().serialize(component);
        int padding = Math.max(0, (CHAT_WIDTH_CHARS - visible.length()) / 2);
        if (padding == 0) {
            return component;
        }
        StringBuilder spaces = new StringBuilder(padding);
        for (int i = 0; i < padding; i++) spaces.append(' ');
        return Component.text(spaces.toString()).append(component);
    }
}

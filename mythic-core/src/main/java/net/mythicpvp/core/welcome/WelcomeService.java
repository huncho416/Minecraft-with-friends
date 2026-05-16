package net.mythicpvp.core.welcome;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WelcomeService {

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
        for (String line : config.getStringList("welcome.lines")) {
            out.add(MythicHex.colorize(line.replace("%player%", player.getName())));
        }
        ConfigurationSection root = config.getConfig().getConfigurationSection("welcome");
        if (root != null) {
            List<?> linkList = root.getList("links");
            if (linkList != null) {
                for (Object obj : linkList) {
                    if (!(obj instanceof java.util.Map<?, ?> raw)) continue;
                    Object label = raw.get("label");
                    Object url = raw.get("url");
                    if (label == null || url == null) continue;
                    Component link = MythicHex.colorize(String.valueOf(label).replace("%player%", player.getName()))
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
        return out;
    }

    public void send(@NotNull Player player) {
        for (Component line : render(player)) {
            player.sendMessage(line);
        }
    }
}

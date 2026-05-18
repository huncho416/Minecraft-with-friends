package net.mythicpvp.core.command;

import net.mythicpvp.core.disguise.DisguiseApplier;
import net.mythicpvp.core.disguise.MojangSkinService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@CommandAlias("disguisemodify|dmodify")
@CommandPermission("mythic.core.disguise")
public final class DisguiseModifyCommand extends MythicCommand {

    private final DisguiseApplier applier;
    private final MojangSkinService skins;
    private final JavaPlugin plugin;

    public DisguiseModifyCommand(@NotNull DisguiseApplier applier,
                                 @NotNull MojangSkinService skins,
                                 @NotNull JavaPlugin plugin) {
        this.applier = applier;
        this.skins = skins;
        this.plugin = plugin;
    }

    @Default
    public void execute(@NotNull Player sender, @NotNull String[] words) {
        if (words.length == 0) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AUsage: &#FFFFFF/disguisemodify name=<name> skin=<minecraft-name|clear> rank=<rank|clear>"));
            return;
        }
        if (!applier.isDisguised(sender.getUniqueId())) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AYou are not currently disguised. Use &#FFFFFF/disguise&#FF8A8A first."));
            return;
        }
        DisguiseManager.DisguiseData current = DisguiseManager.getInstance().getDisguise(sender.getUniqueId());
        if (current == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8ANo active disguise found."));
            return;
        }

        Holder holder = new Holder(current.displayName(), current.skinValue(), current.skinSignature(),
                current.rankOverride());

        String pendingSkinSource = null;
        for (String arg : words) {
            int eq = arg.indexOf('=');
            if (eq <= 0) {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8AArgument &#FFFFFF" + arg + "&#FF8A8A must be key=value."));
                return;
            }
            String key = arg.substring(0, eq).toLowerCase(Locale.ROOT);
            String value = arg.substring(eq + 1);
            switch (key) {
                case "name" -> holder.name = value;
                case "rank" -> holder.rank = value.equalsIgnoreCase("clear") ? null : value;
                case "skin" -> {
                    if (value.equalsIgnoreCase("clear")) {
                        holder.skinValue = null;
                        holder.skinSignature = null;
                        pendingSkinSource = null;
                    } else {
                        pendingSkinSource = value;
                    }
                }
                default -> {
                    sender.sendMessage(MythicHex.colorize(
                            "&#FF8A8AUnknown key &#FFFFFF" + key + "&#FF8A8A. Valid: name, skin, rank."));
                    return;
                }
            }
        }

        if (pendingSkinSource == null) {
            apply(sender, holder);
            return;
        }
        String source = pendingSkinSource;
        sender.sendMessage(MythicHex.colorize(
                "&#D2D8E0Fetching skin for &#FFFFFF" + source + "&#D2D8E0…"));
        skins.lookup(source).thenAccept(result -> MythicScheduler.runSync(plugin, () -> {
            if (!result.success()) {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8ASkin lookup failed for &#FFFFFF" + source + "&#FF8A8A; keeping previous skin."));
            } else {
                holder.skinValue = result.skinValue();
                holder.skinSignature = result.skinSignature();
            }
            apply(sender, holder);
        }));
    }

    private void apply(@NotNull Player sender, @NotNull Holder holder) {
        applier.apply(sender, holder.name, holder.skinValue, holder.skinSignature, holder.rank);
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CDisguise updated &#D2D8E0(name=&#FFFFFF" + holder.name
                        + "&#D2D8E0 rank=&#FFFFFF" + (holder.rank == null ? "none" : holder.rank)
                        + "&#D2D8E0 skin=&#FFFFFF" + (holder.skinValue == null ? "default" : "set")
                        + "&#D2D8E0)."));
    }

    private static final class Holder {
        @NotNull String name;
        @Nullable String skinValue;
        @Nullable String skinSignature;
        @Nullable String rank;

        Holder(@NotNull String name, @Nullable String skinValue, @Nullable String skinSignature, @Nullable String rank) {
            this.name = name;
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
            this.rank = rank;
        }
    }
}

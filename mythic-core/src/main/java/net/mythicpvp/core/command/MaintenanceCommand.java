package net.mythicpvp.core.command;

import net.mythicpvp.core.maintenance.MaintenanceService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.command.Optional;
import net.mythicpvp.suite.command.Subcommand;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandAlias("maintenance|maintenancemode")
@CommandPermission("mythic.core.maintenance")
public final class MaintenanceCommand extends MythicCommand {

    private final MaintenanceService maintenance;

    public MaintenanceCommand(@NotNull MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @Default
    public void toggle(@NotNull CommandSender sender, @Optional String mode) {
        boolean target;
        if (mode == null || mode.isBlank()) {
            target = !maintenance.isActive();
        } else {
            String norm = mode.trim().toLowerCase(java.util.Locale.ROOT);
            target = switch (norm) {
                case "on", "true", "enable", "enabled" -> true;
                case "off", "false", "disable", "disabled" -> false;
                default -> !maintenance.isActive();
            };
        }
        maintenance.setActive(target);
        broadcastChange(sender, target);
    }

    @Subcommand("status")
    public void status(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize(
                "&#FFFFFFMaintenance mode is currently " + (maintenance.isActive() ? "&#FF8A8AON" : "&#9CFF9COFF")
                        + "&#FFFFFF. Bypass list: &#FFFFFF" + maintenance.bypassUuids().size() + " entries."));
    }

    @Subcommand("bypass add")
    @Complete({"players"})
    public void addBypass(@NotNull CommandSender sender, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        boolean added = maintenance.addBypass(target.getUniqueId());
        sender.sendMessage(MythicHex.colorize(added
                ? "&#9CFF9CAdded &f" + targetName + " &#9CFF9Cto the maintenance bypass list."
                : "&#FFEC8A" + targetName + " is already on the bypass list."));
    }

    @Subcommand("bypass remove")
    @Complete({"players"})
    public void removeBypass(@NotNull CommandSender sender, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        boolean removed = maintenance.removeBypass(target.getUniqueId());
        sender.sendMessage(MythicHex.colorize(removed
                ? "&#9CFF9CRemoved &f" + targetName + " &#9CFF9Cfrom the maintenance bypass list."
                : "&#FFEC8A" + targetName + " is not on the bypass list."));
    }

    private void broadcastChange(@NotNull CommandSender sender, boolean active) {
        String label = active ? "&#FF8A8A&lON" : "&#9CFF9C&lOFF";
        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CMaintenance mode is now " + label + "&#9CFF9C."));
        net.kyori.adventure.text.Component notice = MythicHex.colorize(
                "&#FFEC8A&l[Maintenance] &#FFFFFFMaintenance mode " + label + " &#FFFFFF(by " + sender.getName() + ").");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendMessage(notice);
        }
    }
}

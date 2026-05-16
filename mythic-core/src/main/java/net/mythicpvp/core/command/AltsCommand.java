package net.mythicpvp.core.command;

import net.mythicpvp.core.security.IpTracker;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Complete;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CommandAlias("alts")
@CommandPermission("mythic.core.alts")
public final class AltsCommand extends MythicCommand {

    private final IpTracker ipTracker;

    public AltsCommand(@NotNull IpTracker ipTracker) {
        this.ipTracker = ipTracker;
    }

    @Default
    @Complete({"players"})
    public void execute(@NotNull Player viewer, @NotNull String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        List<IpTracker.Entry> alts = ipTracker.altsOf(target.getUniqueId());
        PaginatedMenu menu = PaginatedMenu.create(6,
                "&#F529BEAlts of &#FFFFFF" + targetName + " &7(&f" + alts.size() + "&7)");
        for (IpTracker.Entry alt : alts) {
            ItemStack head = MythicItem.create(Material.PLAYER_HEAD)
                    .name("&#FFFFFF" + alt.name)
                    .lore(buildAltLore(alt))
                    .build();
            try {
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(alt.uuid));
                    head.setItemMeta(meta);
                }
            } catch (Throwable ignored) {
            }
            menu.addItem(head);
        }
        menu.open(viewer);
        if (alts.isEmpty()) {
            viewer.sendMessage(MythicHex.colorize(
                    "&#FFEC8ANo alts recorded for &#FFFFFF" + targetName + "&#FFEC8A."));
        }
    }

    @NotNull
    private List<String> buildAltLore(@NotNull IpTracker.Entry alt) {
        java.util.ArrayList<String> lore = new java.util.ArrayList<>();
        lore.add("&7UUID: &f" + alt.uuid);
        lore.add("&7Known IPs: &f" + alt.ips.size());
        for (String ip : alt.ips.keySet()) {
            lore.add("&8• &f" + ip);
            if (lore.size() > 9) {
                lore.add("&7…");
                break;
            }
        }
        return lore;
    }
}

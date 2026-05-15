package net.mythicpvp.core.chat;

import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChatColorMenuService {

    private final ChatColorService chatColors;

    public ChatColorMenuService(@NotNull ChatColorService chatColors) {
        this.chatColors = chatColors;
    }

    public void open(@NotNull Player player) {
        List<ChatColorService.ChatColorOption> options = chatColors.options();
        int rows = Math.max(3, (options.size() + 8) / 9 + 2);
        MythicMenu menu = MythicMenu.create(rows, "&8Chat Color");

        String activeCode = chatColors.colorFor(player.getUniqueId());
        int slot = 10;
        for (ChatColorService.ChatColorOption option : options) {
            boolean unlocked = player.hasPermission(ChatFormatListener.COLOR_WILDCARD)
                    || player.hasPermission(option.permission());
            boolean equipped = unlocked && option.code().equals(activeCode);
            menu.slot(slot, buildOptionItem(option, unlocked, equipped),
                    event -> handleClick(player, option, unlocked, event.getClick()));
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        menu.open(player);
    }

    private void handleClick(@NotNull Player player,
                             @NotNull ChatColorService.ChatColorOption option,
                             boolean unlocked,
                             @NotNull ClickType click) {
        if (!unlocked) {
            player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(
                    "&cYou don't have permission to use the " + option.displayName() + " chat color."));
            return;
        }
        if (click.isLeftClick()) {
            chatColors.setColor(player.getUniqueId(), option.code());
            player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(
                    "&7Chat color set to " + option.code() + option.displayName() + "&7."));
        } else if (click.isRightClick()) {
            chatColors.clear(player.getUniqueId());
            player.sendMessage(net.mythicpvp.suite.hex.MythicHex.colorize(
                    "&7Chat color cleared. Using your rank's default."));
        } else {
            return;
        }
        open(player);
    }

    @NotNull
    private static org.bukkit.inventory.ItemStack buildOptionItem(@NotNull ChatColorService.ChatColorOption option,
                                                                  boolean unlocked,
                                                                  boolean equipped) {
        Material dye = parseMaterial(option.dye());
        MythicItem item = MythicItem.create(dye)
                .name(option.code() + "&l" + option.displayName())
                .lore(
                        "",
                        "&7Preview: " + option.code() + "Hello, world!",
                        "",
                        unlocked
                                ? (equipped
                                    ? "&aEquipped"
                                    : "&eLeft-click &7to equip")
                                : "&cLocked",
                        unlocked && equipped ? "&eRight-click &7to clear" : "");
        if (equipped) {
            item.glow().flags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        return item.build();
    }

    @NotNull
    private static Material parseMaterial(@NotNull String name) {
        Material m = Material.matchMaterial(name);
        return m == null ? Material.GRAY_DYE : m;
    }
}

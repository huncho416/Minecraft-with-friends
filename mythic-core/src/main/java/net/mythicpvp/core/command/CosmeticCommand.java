package net.mythicpvp.core.command;

import net.mythicpvp.core.cosmetic.CosmeticService;
import net.mythicpvp.suite.command.CommandAlias;
import net.mythicpvp.suite.command.CommandPermission;
import net.mythicpvp.suite.command.Default;
import net.mythicpvp.suite.command.MythicCommand;
import net.mythicpvp.suite.cosmetic.CosmeticManager;
import net.mythicpvp.suite.cosmetic.CosmeticType;
import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;

@CommandAlias("cosmetic|cosmeticadmin")
@CommandPermission("mythic.core.cosmetic.admin")
public final class CosmeticCommand extends MythicCommand {

    private final CosmeticService cosmeticService;

    public CosmeticCommand(@NotNull CosmeticService cosmeticService) {
        this.cosmeticService = cosmeticService;
    }

    @Default
    public void execute(@NotNull CommandSender sender, @NotNull String[] words) {
        if (words.length == 0 || words[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return;
        }
        String sub = words[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give")) {
            handleGive(sender, words);
            return;
        }
        if (sub.equals("list")) {
            handleList(sender, words);
            return;
        }
        sendHelp(sender);
    }

    private void handleGive(@NotNull CommandSender sender, @NotNull String[] words) {
        if (words.length < 3) {
            sender.sendMessage(MythicHex.colorize(
                    "&#FF8A8AUsage: &#FFFFFF/cosmetic give <player> <cosmeticId|all|type:HAT|type:CHAT_TAG|...>"));
            return;
        }
        String targetName = words[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getUniqueId() == null) {
            sender.sendMessage(MythicHex.colorize("&#FF8A8AUnknown player &#FFFFFF" + targetName + "&#FF8A8A."));
            return;
        }
        String selector = words[2];

        Collection<CosmeticManager.Cosmetic> all = CosmeticManager.getInstance().getAll();
        Collection<CosmeticManager.Cosmetic> toGrant;
        if (selector.equalsIgnoreCase("all")) {
            toGrant = all;
        } else if (selector.toLowerCase(Locale.ROOT).startsWith("type:")) {
            String typeName = selector.substring(5).toUpperCase(Locale.ROOT);
            CosmeticType type;
            try {
                type = CosmeticType.valueOf(typeName);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8AUnknown cosmetic type &#FFFFFF" + typeName + "&#FF8A8A. Valid: "
                                + validTypeList()));
                return;
            }
            toGrant = CosmeticManager.getInstance().getByType(type);
        } else {
            CosmeticManager.Cosmetic cosmetic = CosmeticManager.getInstance().get(selector);
            if (cosmetic == null) {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8AUnknown cosmetic id &#FFFFFF" + selector + "&#FF8A8A."));
                return;
            }
            toGrant = java.util.List.of(cosmetic);
        }

        int granted = 0;
        int alreadyOwned = 0;
        for (CosmeticManager.Cosmetic cosmetic : toGrant) {
            if (CosmeticManager.getInstance().ownsCosmetic(target.getUniqueId(), cosmetic.id())) {
                alreadyOwned++;
                continue;
            }
            CosmeticManager.getInstance().grantCosmetic(target.getUniqueId(), cosmetic.id());
            cosmeticService.persistGrant(target.getUniqueId(), cosmetic.id(), "ADMIN_GIVE",
                    sender.getName());
            granted++;
        }

        sender.sendMessage(MythicHex.colorize(
                "&#9CFF9CGranted &#FFFFFF" + granted + "&#9CFF9C cosmetic(s) to &#FFFFFF"
                        + target.getName() + "&#9CFF9C. (&#D2D8E0" + alreadyOwned + " already owned&#9CFF9C)"));
    }

    private void handleList(@NotNull CommandSender sender, @NotNull String[] words) {
        if (words.length >= 2 && words[1].toLowerCase(Locale.ROOT).startsWith("type:")) {
            String typeName = words[1].substring(5).toUpperCase(Locale.ROOT);
            CosmeticType type;
            try {
                type = CosmeticType.valueOf(typeName);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(MythicHex.colorize(
                        "&#FF8A8AUnknown cosmetic type &#FFFFFF" + typeName + "&#FF8A8A. Valid: "
                                + validTypeList()));
                return;
            }
            Collection<CosmeticManager.Cosmetic> entries = CosmeticManager.getInstance().getByType(type);
            sender.sendMessage(MythicHex.colorize(
                    "&#F529BE" + type.getDisplayName() + " &#D2D8E0(" + entries.size() + "):"));
            for (CosmeticManager.Cosmetic entry : entries) {
                sender.sendMessage(MythicHex.colorize(
                        "&7- &#FFFFFF" + entry.id() + " &8» &#D2D8E0" + entry.displayName()));
            }
            return;
        }
        sender.sendMessage(MythicHex.colorize("&#F529BECosmetic types &#D2D8E0(use &#FFFFFF/cosmetic list type:<TYPE>&#D2D8E0):"));
        for (CosmeticType type : CosmeticType.values()) {
            int size = CosmeticManager.getInstance().getByType(type).size();
            sender.sendMessage(MythicHex.colorize(
                    "&7- &#FFFFFF" + type.name() + " &8» &#D2D8E0" + size + " entries"));
        }
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(MythicHex.colorize("&#F529BECosmetic admin commands:"));
        sender.sendMessage(MythicHex.colorize(
                "&7- &#FFFFFF/cosmetic give <player> <id|all|type:TYPE> &8» &#D2D8E0Grant cosmetic(s)"));
        sender.sendMessage(MythicHex.colorize(
                "&7- &#FFFFFF/cosmetic list [type:TYPE] &8» &#D2D8E0List cosmetics or types"));
    }

    @NotNull
    private static String validTypeList() {
        StringBuilder sb = new StringBuilder();
        CosmeticType[] values = CosmeticType.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values[i].name());
        }
        return sb.toString();
    }
}

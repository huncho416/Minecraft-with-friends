package net.mythicpvp.core.staffmode;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.core.config.CoreMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class StaffModeToolListener implements Listener {

    private final StaffModeService staff;
    private final CoreMessages messages;
    private final net.mythicpvp.core.rank.GrantService grants;
    private final net.mythicpvp.core.rank.RankService ranks;

    public StaffModeToolListener(
            @NotNull StaffModeService staff,
            @NotNull CoreMessages messages,
            @NotNull net.mythicpvp.core.rank.GrantService grants,
            @NotNull net.mythicpvp.core.rank.RankService ranks) {
        this.staff = staff;
        this.messages = messages;
        this.grants = grants;
        this.ranks = ranks;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!staff.inStaffMode(player.getUniqueId())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        StaffModeTool tool = matchTool(item);
        if (tool == null) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        event.setCancelled(true);
        switch (tool.type()) {
            case "INSPECT" -> handleInspect(player, target);
            case "FREEZE" -> handleFreeze(player, target);
            default -> {  }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staff.inStaffMode(player.getUniqueId())) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        StaffModeTool tool = matchTool(item);
        if (tool == null) {
            return;
        }

        switch (tool.type()) {
            case "RANDOM_TELEPORT" -> {
                event.setCancelled(true);
                handleRandomTeleport(player);
            }
            case "DISABLE" -> {
                event.setCancelled(true);
                staff.disable(player);
            }
            default -> {  }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!staff.isFrozen(event.getPlayer().getUniqueId())) {
            return;
        }

        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {

        staff.onQuit(event.getPlayer());
    }

    private void handleInspect(@NotNull Player staffPlayer, @NotNull Player target) {
        String rankId = grants.activeRank(target.getUniqueId());
        var rank = ranks.get(rankId);
        String rankName = rank == null ? "default" : rank.name();
        staffPlayer.sendMessage(messages.component(
                "messages.staff-mode.inspect",
                "&#FF00F8Inspect &8» &#FFFFFF%target%: rank=%rank% gamemode=%gamemode%",
                Map.of(
                        "target", target.getName(),
                        "rank", rankName,
                        "gamemode", target.getGameMode().name())));
    }

    private void handleFreeze(@NotNull Player staffPlayer, @NotNull Player target) {
        boolean nowFrozen = staff.toggleFreeze(target.getUniqueId());
        staffPlayer.sendMessage(messages.component(
                nowFrozen ? "messages.staff-mode.frozen" : "messages.staff-mode.unfrozen",
                nowFrozen
                        ? "&#FF00F8Freeze &8» &#FFFFFFFroze %target%."
                        : "&#FF00F8Freeze &8» &#FFFFFFUnfroze %target%.",
                Map.of("target", target.getName())));
    }

    private void handleRandomTeleport(@NotNull Player staffPlayer) {
        var candidates = staffPlayer.getServer().getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(staffPlayer.getUniqueId()))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        Player chosen = candidates.get((int) (Math.random() * candidates.size()));
        staffPlayer.teleport(chosen.getLocation());
    }

    private StaffModeTool matchTool(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        for (StaffModeTool tool : staff.tools()) {

            String configuredPlain = stripHex(tool.name());
            if (configuredPlain.equals(displayName) && item.getType() == tool.material()) {
                return tool;
            }
        }
        return null;
    }

    @NotNull
    private static String stripHex(@NotNull String input) {

        return input.replaceAll("&#[0-9A-Fa-f]{6}", "")
                .replaceAll("&[0-9a-fk-or]", "")
                .trim();
    }
}

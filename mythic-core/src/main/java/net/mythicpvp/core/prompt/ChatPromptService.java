package net.mythicpvp.core.prompt;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ChatPromptService implements Listener {

    private final Map<UUID, BiConsumer<Player, String>> prompts = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public ChatPromptService(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void await(@NotNull Player player, @NotNull BiConsumer<Player, String> handler) {
        prompts.put(player.getUniqueId(), handler);
        player.closeInventory();
        player.sendMessage("Enter the value in chat, or type cancel.");
    }

    public void cancel(@NotNull UUID player) {
        prompts.remove(player);
    }

    public boolean waiting(@NotNull UUID player) {
        return prompts.containsKey(player);
    }

    @EventHandler
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        BiConsumer<Player, String> handler = prompts.remove(event.getPlayer().getUniqueId());
        if (handler == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        if (!message.equalsIgnoreCase("cancel")) {
            if (event.isAsynchronous()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> handler.accept(event.getPlayer(), message));
            } else {
                handler.accept(event.getPlayer(), message);
            }
        }
    }
}

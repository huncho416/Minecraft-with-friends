package net.mythicpvp.core.prompt;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ChatPromptService implements Listener {

    private final Map<UUID, BiConsumer<Player, String>> prompts = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;

    public ChatPromptService(@NotNull JavaPlugin plugin) {
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
    public void onQuit(@NotNull PlayerQuitEvent event) {

        prompts.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(@NotNull AsyncChatEvent event) {
        BiConsumer<Player, String> handler = prompts.remove(event.getPlayer().getUniqueId());
        if (handler == null) {
            return;
        }
        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (!message.equalsIgnoreCase("cancel")) {
            if (event.isAsynchronous()) {

                MythicScheduler.runSync(plugin, () -> handler.accept(event.getPlayer(), message));
            } else {
                handler.accept(event.getPlayer(), message);
            }
        }
    }
}

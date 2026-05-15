package net.mythicpvp.core.transfer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ProxyTransferService {

    public static final String CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;

    public ProxyTransferService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Messenger messenger = plugin.getServer().getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        }
    }

    public boolean transfer(@NotNull Player player, @NotNull String targetShardId) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeUTF("Connect");
            out.writeUTF(targetShardId);
        } catch (IOException e) {
            plugin.getLogger().warning("[transfer] failed to encode Connect for " + targetShardId + ": " + e.getMessage());
            return false;
        }
        player.sendPluginMessage(plugin, CHANNEL, buffer.toByteArray());
        return true;
    }
}

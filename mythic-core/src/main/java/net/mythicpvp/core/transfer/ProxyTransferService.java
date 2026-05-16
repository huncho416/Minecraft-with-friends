package net.mythicpvp.core.transfer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Transfers a player to another shard by issuing a Paper Server Transfer
 * packet (1.20.5+) targeting {@code <shard>.<proxy-domain>:25565}. The
 * client reconnects directly to that hostname, the proxy reads the
 * handshake hostname, and the proxy's routing table (populated from
 * {@code servers/<shard>.toml}) forwards the connection to the backend.
 *
 * <p>The legacy BungeeCord {@code Connect} plugin message is still
 * registered for back-compat with operator-side commands that emit it
 * directly, but the {@link #transfer(Player, String)} path no longer
 * relies on it because Infrarust v2.0.0-alpha.6 does not consume that
 * channel.
 */
public final class ProxyTransferService {

    public static final String CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;
    private volatile String proxyDomain = "";
    private volatile int proxyPort = 25565;

    public ProxyTransferService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Messenger messenger = plugin.getServer().getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        }
    }

    public void setProxyDomain(@NotNull String domain, int port) {
        this.proxyDomain = domain.trim();
        this.proxyPort = port;
    }

    @NotNull
    public String proxyDomain() {
        return proxyDomain;
    }

    public boolean transfer(@NotNull Player player, @NotNull String targetShardId) {
        String hostname = resolveHostname(targetShardId);
        try {
            player.transfer(hostname, proxyPort);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning(
                    "[transfer] Player.transfer to " + hostname + ":" + proxyPort
                            + " failed: " + t.getMessage() + " — falling back to BungeeCord Connect");
            return sendBungeeConnect(player, targetShardId);
        }
    }

    @NotNull
    private String resolveHostname(@NotNull String shardId) {
        String suffix = proxyDomain;
        if (suffix == null || suffix.isBlank()) {
            return shardId;
        }
        if (suffix.contains(",")) {
            suffix = suffix.split(",", 2)[0].trim();
        }
        return shardId + "." + suffix;
    }

    private boolean sendBungeeConnect(@NotNull Player player, @NotNull String shard) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeUTF("Connect");
            out.writeUTF(shard);
        } catch (IOException e) {
            plugin.getLogger().warning("[transfer] failed to encode Connect for " + shard + ": " + e.getMessage());
            return false;
        }
        player.sendPluginMessage(plugin, CHANNEL, buffer.toByteArray());
        return true;
    }
}

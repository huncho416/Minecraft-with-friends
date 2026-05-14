package net.mythicpvp.suite.resourcepack;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackManager {

    private static final ResourcePackManager INSTANCE = new ResourcePackManager();
    private final Map<String, CustomModel> models = new ConcurrentHashMap<>();
    private final Map<String, String> fonts = new ConcurrentHashMap<>();
    private final Map<UUID, PackDelivery> deliveries = new ConcurrentHashMap<>();
    private volatile String packUrl = "";
    private volatile String packHash = "";
    private volatile String bedrockPackUrl = "";
    private volatile String bedrockPackHash = "";
    private volatile boolean forceUpdate;
    private volatile BedrockPackConverter bedrockConverter = javaPack -> CompletableFuture.completedFuture(javaPack);

    private ResourcePackManager() {}

    @NotNull
    public static ResourcePackManager getInstance() {
        return INSTANCE;
    }

    public void setPackInfo(@NotNull String url, @NotNull String hash) {
        this.packUrl = url;
        this.packHash = hash;
    }

    public void setBedrockPackInfo(@NotNull String url, @NotNull String hash) {
        this.bedrockPackUrl = url;
        this.bedrockPackHash = hash;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setBedrockConverter(@NotNull BedrockPackConverter converter) {
        this.bedrockConverter = converter;
    }

    @NotNull
    public CompletableFuture<Path> convertForBedrock(@NotNull Path javaPack) {
        return bedrockConverter.convert(javaPack);
    }

    public void sendTo(@NotNull Player player) {
        if (packUrl.isBlank()) {
            throw new IllegalStateException("Resource pack URL is not configured");
        }

        UUID packId = UUID.nameUUIDFromBytes(packUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                .id(packId)
                .uri(URI.create(packUrl))
                .hash(packHash == null ? "" : packHash)
                .build();
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(forceUpdate)
                .replace(true)
                .build();
        player.sendResourcePacks(request);
        deliveries.put(player.getUniqueId(), new PackDelivery(packUrl, packHash, System.currentTimeMillis(), forceUpdate));
    }

    public void resendTo(@NotNull Player player) {
        sendTo(player);
    }

    public void hotSwap(@NotNull String url, @NotNull String hash) {
        setPackInfo(url, hash);
        forceUpdate = true;
    }

    @NotNull
    public Optional<PackDelivery> getDelivery(@NotNull UUID player) {
        return Optional.ofNullable(deliveries.get(player));
    }

    @NotNull
    public Map<UUID, PackDelivery> getDeliveries() {
        return Collections.unmodifiableMap(deliveries);
    }

    @NotNull
    public String getPackUrl() {
        return packUrl;
    }

    @NotNull
    public String getPackHash() {
        return packHash;
    }

    @NotNull
    public String getBedrockPackUrl() {
        return bedrockPackUrl;
    }

    @NotNull
    public String getBedrockPackHash() {
        return bedrockPackHash;
    }

    public void registerModel(@NotNull String id, @NotNull Material material, @NotNull NamespacedKey itemModel) {
        models.put(normalize(id), new CustomModel(normalize(id), material, itemModel));
    }

    @Nullable
    public CustomModel getModel(@NotNull String id) {
        return models.get(normalize(id));
    }

    @NotNull
    public Map<String, CustomModel> getModels() {
        return Collections.unmodifiableMap(models);
    }

    public void registerFont(@NotNull String id, @NotNull String namespacedKey) {
        fonts.put(normalize(id), namespacedKey);
    }

    @Nullable
    public String getFont(@NotNull String id) {
        return fonts.get(normalize(id));
    }

    @NotNull
    public Map<String, String> getFonts() {
        return Collections.unmodifiableMap(fonts);
    }

    @NotNull
    public String computeHash(@NotNull Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Unable to compute resource pack hash", e);
        }
    }

    public void clear() {
        models.clear();
        fonts.clear();
        deliveries.clear();
        packUrl = "";
        packHash = "";
        bedrockPackUrl = "";
        bedrockPackHash = "";
        forceUpdate = false;
        bedrockConverter = javaPack -> CompletableFuture.completedFuture(javaPack);
    }

    @NotNull
    private String normalize(@NotNull String id) {
        String normalized = id.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Resource pack id cannot be blank");
        }
        return normalized;
    }

    public record CustomModel(@NotNull String id, @NotNull Material material, @NotNull NamespacedKey itemModel) {}

    public record PackDelivery(@NotNull String url, @NotNull String hash, long sentAtMillis, boolean forced) {}
}

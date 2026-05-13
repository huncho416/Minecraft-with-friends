package net.mythicpvp.suite.resourcepack;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackManager {

    private static final ResourcePackManager INSTANCE = new ResourcePackManager();
    private final Map<String, CustomModel> models = new ConcurrentHashMap<>();
    private final Map<String, String> fonts = new ConcurrentHashMap<>();
    private String packUrl = "";
    private String packHash = "";

    private ResourcePackManager() {}

    @NotNull
    public static ResourcePackManager getInstance() {
        return INSTANCE;
    }

    public void setPackInfo(@NotNull String url, @NotNull String hash) {
        this.packUrl = url;
        this.packHash = hash;
    }

    @NotNull public String getPackUrl() { return packUrl; }
    @NotNull public String getPackHash() { return packHash; }

    public void registerModel(@NotNull String id, @NotNull Material material, int customModelData) {
        models.put(id.toLowerCase(), new CustomModel(id, material, customModelData));
    }

    @Nullable
    public CustomModel getModel(@NotNull String id) {
        return models.get(id.toLowerCase());
    }

    public void registerFont(@NotNull String id, @NotNull String namespacedKey) {
        fonts.put(id.toLowerCase(), namespacedKey);
    }

    @Nullable
    public String getFont(@NotNull String id) {
        return fonts.get(id.toLowerCase());
    }

    public record CustomModel(@NotNull String id, @NotNull Material material, int customModelData) {}
}

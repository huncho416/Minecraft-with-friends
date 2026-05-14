package net.mythicpvp.suite.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinManager {

    private static final SkinManager INSTANCE = new SkinManager();
    private static final String MOJANG_SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private final Map<UUID, SkinData> cache = new ConcurrentHashMap<>();

    private SkinManager() {}

    @NotNull
    public static SkinManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    public CompletableFuture<SkinData> fetch(@NotNull UUID uuid) {
        SkinData cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = MOJANG_SESSION_URL + uuid.toString().replace("-", "") + "?unsigned=false";
                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) return SkinData.EMPTY;

                JsonObject json = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
                JsonObject properties = json.getAsJsonArray("properties").get(0).getAsJsonObject();

                SkinData data = new SkinData(
                        properties.get("value").getAsString(),
                        properties.get("signature").getAsString()
                );
                cache.put(uuid, data);
                return data;
            } catch (Exception e) {
                return SkinData.EMPTY;
            }
        });
    }

    public void cache(@NotNull UUID uuid, @NotNull SkinData data) {
        cache.put(uuid, data);
    }

    public void invalidate(@NotNull UUID uuid) {
        cache.remove(uuid);
    }

    public void clearCache() {
        cache.clear();
    }

    public record SkinData(@NotNull String value, @NotNull String signature) {
        public static final SkinData EMPTY = new SkinData("", "");

        public boolean isEmpty() {
            return value.isEmpty();
        }
    }
}

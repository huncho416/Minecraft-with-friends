package net.mythicpvp.suite.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public final class GsonSpacetimeCodec implements SpacetimeCodec {

    private final Gson gson = new GsonBuilder().create();

    @Override
    public byte @NotNull [] encode(@NotNull Object value) {
        return gson.toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @NotNull
    public <T> T decode(byte @NotNull [] bytes, @NotNull Class<T> type) {
        return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
    }
}

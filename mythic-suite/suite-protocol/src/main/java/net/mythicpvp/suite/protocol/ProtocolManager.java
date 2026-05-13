package net.mythicpvp.suite.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ProtocolManager {

    private static final ProtocolManager INSTANCE = new ProtocolManager();
    private static final Gson GSON = new GsonBuilder().create();
    private final Map<String, List<Consumer<ProtocolMessage>>> subscribers = new ConcurrentHashMap<>();

    private ProtocolManager() {}

    @NotNull
    public static ProtocolManager getInstance() {
        return INSTANCE;
    }

    public <T> void subscribe(@NotNull String channel, @NotNull Consumer<ProtocolMessage> handler) {
        subscribers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void publish(@NotNull String channel, @NotNull Object payload) {
        String json = GSON.toJson(payload);
        ProtocolMessage message = new ProtocolMessage(channel, json, System.currentTimeMillis());
        List<Consumer<ProtocolMessage>> handlers = subscribers.get(channel);
        if (handlers != null) {
            handlers.forEach(h -> h.accept(message));
        }
    }

    public void unsubscribeAll(@NotNull String channel) {
        subscribers.remove(channel);
    }

    public void clear() {
        subscribers.clear();
    }

    public record ProtocolMessage(@NotNull String channel, @NotNull String payload, long timestamp) {
        public <T> T deserialize(@NotNull Class<T> type) {
            return GSON.fromJson(payload, type);
        }
    }
}

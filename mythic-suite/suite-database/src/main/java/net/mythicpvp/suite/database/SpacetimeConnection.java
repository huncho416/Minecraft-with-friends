package net.mythicpvp.suite.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class SpacetimeConnection implements WebSocket.Listener {

    private final String uri;
    private final String moduleName;
    private WebSocket webSocket;
    private final HttpClient httpClient;
    private final Map<String, Consumer<String>> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean connected = false;
    private volatile boolean shouldReconnect = true;
    private final StringBuilder messageBuffer = new StringBuilder();

    public SpacetimeConnection(@NotNull String uri, @NotNull String moduleName) {
        this.uri = uri;
        this.moduleName = moduleName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @NotNull
    public CompletableFuture<Void> connect() {
        String wsUri = uri.replace("http://", "ws://").replace("https://", "wss://")
                + "/database/subscribe/" + moduleName;

        return httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(wsUri), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    this.connected = true;
                });
    }

    public void disconnect() {
        shouldReconnect = false;
        connected = false;
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        reconnectExecutor.shutdown();
    }

    public boolean isConnected() {
        return connected;
    }

    public void callReducer(@NotNull String reducerName, @NotNull String argsJson) {
        if (!connected || webSocket == null) return;
        String message = "{\"call\":{\"fn\":\"" + reducerName + "\",\"args\":" + argsJson + "}}";
        webSocket.sendText(message, true);
    }

    public void subscribe(@NotNull String tableName, @NotNull Consumer<String> handler) {
        subscriptions.put(tableName, handler);
        if (connected && webSocket != null) {
            String message = "{\"subscribe\":{\"query_strings\":[\"SELECT * FROM " + tableName + "\"]}}";
            webSocket.sendText(message, true);
        }
    }

    @Override
    public void onOpen(WebSocket ws) {
        connected = true;
        subscriptions.forEach((table, handler) -> {
            String message = "{\"subscribe\":{\"query_strings\":[\"SELECT * FROM " + table + "\"]}}";
            ws.sendText(message, true);
        });
        ws.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        messageBuffer.append(data);
        if (last) {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0);
            handleMessage(message);
        }
        ws.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        connected = false;
        if (shouldReconnect) {
            reconnectExecutor.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        connected = false;
        if (shouldReconnect) {
            reconnectExecutor.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
    }

    private void reconnect() {
        if (!shouldReconnect) return;
        connect().exceptionally(e -> {
            reconnectExecutor.schedule(this::reconnect, 10, TimeUnit.SECONDS);
            return null;
        });
    }

    private void handleMessage(@NotNull String message) {
        for (Map.Entry<String, Consumer<String>> entry : subscriptions.entrySet()) {
            if (message.contains(entry.getKey())) {
                entry.getValue().accept(message);
            }
        }
    }
}

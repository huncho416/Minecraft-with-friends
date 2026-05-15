package net.mythicpvp.suite.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class SpacetimeConnection implements WebSocket.Listener {

    private static final Gson GSON = new GsonBuilder().create();
    private final String uri;
    private final String moduleName;
    private final HttpClient httpClient;
    private final Map<String, List<Consumer<TableEvent>>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ReducerResult>> reducerCalls = new ConcurrentHashMap<>();
    private final List<Consumer<Boolean>> stateListeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong requestIds = new AtomicLong();
    private final StringBuilder messageBuffer = new StringBuilder();
    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile boolean shouldReconnect = true;

    public SpacetimeConnection(@NotNull String uri, @NotNull String moduleName) {
        this.uri = uri;
        this.moduleName = moduleName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @NotNull
    public CompletableFuture<Void> connect() {
        URI websocketUri = URI.create(websocketBaseUri() + "/v1/database/" + path(moduleName) + "/subscribe");
        return httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .subprotocols("v1.json.spacetimedb")
                .buildAsync(websocketUri, this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    setConnected(true);
                });
    }

    public void disconnect() {
        shouldReconnect = false;
        setConnected(false);
        failPendingReducers("SpacetimeDB connection closed");
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        reconnectExecutor.shutdownNow();
    }

    public boolean isConnected() {
        return connected;
    }

    public void onStateChange(@NotNull Consumer<Boolean> listener) {
        stateListeners.add(listener);
    }

    @NotNull
    public CompletableFuture<ReducerResult> callReducer(@NotNull String reducerName, @NotNull Object args) {
        ensureIdentifier(reducerName, "Reducer name");
        String requestId = UUID.randomUUID().toString();
        if (!isConnected()) {
            CompletableFuture<ReducerResult> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("SpacetimeDB connection is not connected"));
            return future;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(httpBaseUri() + "/v1/database/" + path(moduleName) + "/call/" + path(reducerName)))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(args)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                    String error = success ? null : "HTTP " + response.statusCode() + ": " + response.body();
                    return new ReducerResult(requestId, success, response.body(), error);
                });
    }

    @NotNull
    public CompletableFuture<ReducerResult> callReducer(@NotNull String reducerName, @NotNull String argsJson) {
        return callReducer(reducerName, GSON.fromJson(argsJson, Object.class));
    }

    public void subscribe(@NotNull String tableName, @NotNull Consumer<String> handler) {
        subscribeTable(tableName, event -> handler.accept(event.payload()));
    }

    public void subscribeTable(@NotNull String tableName, @NotNull Consumer<TableEvent> handler) {
        ensureIdentifier(tableName, "Table name");
        subscriptions.computeIfAbsent(tableName, key -> new CopyOnWriteArrayList<>()).add(handler);
        send(subscription(tableName, requestIds.incrementAndGet()));
    }

    @NotNull
    public String reducerMessage(@NotNull String reducerName, @NotNull Object args, @NotNull String requestId) {
        ensureIdentifier(reducerName, "Reducer name");
        return GSON.toJson(Map.of("CallReducer", Map.of(
                "reducer", reducerName,
                "args", GSON.toJson(args),
                "request_id", requestId,
                "flags", 0)));
    }

    @NotNull
    public String subscriptionMessage(@NotNull String tableName) {
        ensureIdentifier(tableName, "Table name");
        return GSON.toJson(subscription(tableName, 1));
    }

    @Override
    public void onOpen(WebSocket ws) {
        this.webSocket = ws;
        setConnected(true);
        subscriptions.keySet().forEach(table -> ws.sendText(
                GSON.toJson(subscription(table, requestIds.incrementAndGet())),
                true));
        ws.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        String message = null;
        synchronized (messageBuffer) {
            messageBuffer.append(data);
            if (last) {
                message = messageBuffer.toString();
                messageBuffer.setLength(0);
            }
        }
        if (message != null) {
            handleMessage(message);
        }
        ws.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        setConnected(false);
        failPendingReducers("SpacetimeDB connection closed");
        if (shouldReconnect) {
            reconnectExecutor.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        setConnected(false);
        failPendingReducers("SpacetimeDB connection errored");
        if (shouldReconnect) {
            reconnectExecutor.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
    }

    private void failPendingReducers(@NotNull String message) {
        reducerCalls.forEach((requestId, future) -> future.completeExceptionally(new IllegalStateException(message)));
        reducerCalls.clear();
    }

    private void reconnect() {
        if (!shouldReconnect) return;
        connect().exceptionally(error -> {
            reconnectExecutor.schedule(this::reconnect, 10, TimeUnit.SECONDS);
            return null;
        });
    }

    private boolean send(@NotNull Object message) {
        WebSocket current = webSocket;
        if (!connected || current == null) {
            return false;
        }
        current.sendText(GSON.toJson(message), true);
        return true;
    }

    @NotNull
    private Map<String, Object> subscription(@NotNull String tableName, long requestId) {
        return Map.of("Subscribe", Map.of(
                "query_strings", List.of("SELECT * FROM " + tableName),
                "request_id", requestId));
    }

    @NotNull
    private String httpBaseUri() {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    @NotNull
    private String websocketBaseUri() {
        return httpBaseUri().replace("http://", "ws://").replace("https://", "wss://");
    }

    @NotNull
    private static String path(@NotNull String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void handleMessage(@NotNull String message) {
        JsonObject root;
        try {
            root = JsonParser.parseString(message).getAsJsonObject();
        } catch (IllegalStateException | JsonSyntaxException e) {
            return;
        }
        if (root.has("requestId")) {
            String requestId = root.get("requestId").getAsString();
            CompletableFuture<ReducerResult> future = reducerCalls.remove(requestId);
            if (future != null) {
                boolean success = !root.has("error") || root.get("error").isJsonNull();
                String payload = root.has("payload") ? root.get("payload").toString() : null;
                String error = root.has("error") && !root.get("error").isJsonNull() ? root.get("error").getAsString() : null;
                future.complete(new ReducerResult(requestId, success, payload, error));
            }
        }
        if (root.has("table")) {
            String table = root.get("table").getAsString();
            String payload = root.has("payload") ? root.get("payload").toString() : message;
            String operation = root.has("operation") ? root.get("operation").getAsString() : "update";
            TableEvent event = new TableEvent(table, payload, operation);
            subscriptions.getOrDefault(table, List.of()).forEach(handler -> handler.accept(event));
        }
    }

    private void setConnected(boolean value) {
        connected = value;
        stateListeners.forEach(listener -> listener.accept(value));
    }

    private void ensureIdentifier(@NotNull String value, @NotNull String label) {
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(label + " must be a simple identifier");
        }
    }

}

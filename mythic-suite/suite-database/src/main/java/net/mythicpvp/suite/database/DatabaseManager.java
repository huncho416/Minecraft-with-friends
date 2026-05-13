package net.mythicpvp.suite.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DatabaseManager {

    private static final DatabaseManager INSTANCE = new DatabaseManager();
    private final Map<String, SpacetimeConnection> connections = new ConcurrentHashMap<>();
    private SpacetimeConnection primary;

    private DatabaseManager() {}

    @NotNull
    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    public SpacetimeConnection createConnection(@NotNull String name, @NotNull String uri, @NotNull String moduleName) {
        SpacetimeConnection connection = new SpacetimeConnection(uri, moduleName);
        connections.put(name, connection);
        if (primary == null) {
            primary = connection;
        }
        return connection;
    }

    @Nullable
    public SpacetimeConnection getConnection(@NotNull String name) {
        return connections.get(name);
    }

    @NotNull
    public SpacetimeConnection getPrimary() {
        if (primary == null) {
            throw new IllegalStateException("No database connection established");
        }
        return primary;
    }

    public void disconnectAll() {
        connections.values().forEach(SpacetimeConnection::disconnect);
        connections.clear();
        primary = null;
    }
}

package net.mythicpvp.suite.api.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistry {

    private static final ServiceRegistry INSTANCE = new ServiceRegistry();
    private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();

    private ServiceRegistry() {}

    @NotNull
    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }

    public <T extends Service> void register(@NotNull Class<T> type, @NotNull T service) {
        services.put(type, service);
        service.initialize();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Service> T get(@NotNull Class<T> type) {
        return (T) services.get(type);
    }

    @NotNull
    public <T extends Service> T require(@NotNull Class<T> type) {
        T service = get(type);
        if (service == null) {
            throw new IllegalStateException("Service not registered: " + type.getSimpleName());
        }
        return service;
    }

    public void shutdownAll() {
        services.values().forEach(Service::shutdown);
        services.clear();
    }
}

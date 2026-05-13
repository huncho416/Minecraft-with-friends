package net.mythicpvp.suite.event;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {

    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<? extends MythicEvent>, List<RegisteredHandler>> handlers = new ConcurrentHashMap<>();

    private EventBus() {}

    @NotNull
    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void register(@NotNull Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            MythicHandler annotation = method.getAnnotation(MythicHandler.class);
            if (annotation == null) continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !MythicEvent.class.isAssignableFrom(params[0])) continue;

            @SuppressWarnings("unchecked")
            Class<? extends MythicEvent> eventType = (Class<? extends MythicEvent>) params[0];
            method.setAccessible(true);

            handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(new RegisteredHandler(listener, method, annotation.priority(), annotation.ignoreCancelled()));

            handlers.get(eventType).sort(Comparator.comparingInt(RegisteredHandler::priority));
        }
    }

    public void unregister(@NotNull Object listener) {
        handlers.values().forEach(list ->
                list.removeIf(h -> h.listener() == listener));
    }

    @NotNull
    public <T extends MythicEvent> T fire(@NotNull T event) {
        List<RegisteredHandler> list = handlers.get(event.getClass());
        if (list == null) return event;

        for (RegisteredHandler handler : list) {
            if (handler.ignoreCancelled() && event.isCancelled()) continue;
            try {
                handler.method().invoke(handler.listener(), event);
            } catch (Exception e) {
                throw new RuntimeException("Error dispatching event " + event.getEventName(), e);
            }
        }
        return event;
    }

    public void clear() {
        handlers.clear();
    }

    private record RegisteredHandler(Object listener, Method method, int priority, boolean ignoreCancelled) {}
}

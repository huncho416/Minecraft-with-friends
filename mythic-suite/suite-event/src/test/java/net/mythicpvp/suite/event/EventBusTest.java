package net.mythicpvp.suite.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    private final EventBus bus = EventBus.getInstance();

    @AfterEach
    void cleanup() {
        bus.clear();
    }

    @Test
    void firesRegisteredHandlersInPriorityOrder() {
        List<Integer> calls = new ArrayList<>();
        Object listener = new Object() {
            @MythicHandler(priority = 10)
            public void late(TestEvent event) {
                calls.add(10);
            }

            @MythicHandler(priority = 1)
            public void early(TestEvent event) {
                calls.add(1);
            }
        };
        bus.register(listener);
        bus.fire(new TestEvent());
        assertEquals(List.of(1, 10), calls);
    }

    static final class TestEvent extends MythicEvent {}
}

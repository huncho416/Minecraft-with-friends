package net.mythicpvp.suite.api.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceRegistryTest {

    private final ServiceRegistry registry = ServiceRegistry.getInstance();

    @AfterEach
    void cleanup() {
        registry.shutdownAll();
    }

    @Test
    void initializesAndShutsDownServices() {
        TestService service = new TestService();
        registry.register(TestService.class, service);
        assertSame(service, registry.require(TestService.class));
        assertTrue(service.initialized);
        registry.shutdownAll();
        assertTrue(service.shutdown);
    }

    static final class TestService implements Service {
        boolean initialized;
        boolean shutdown;

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }
    }
}

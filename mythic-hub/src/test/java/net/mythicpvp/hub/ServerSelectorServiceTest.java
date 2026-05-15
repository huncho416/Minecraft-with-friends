package net.mythicpvp.hub;

import net.mythicpvp.hub.selector.ServerSelectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerSelectorServiceTest {

    private ServerSelectorService service;

    @BeforeEach
    void setUp() {
        service = new ServerSelectorService();
    }

    @Test
    void filtersByRole() {
        service.updateServer("survival-1", "survival", 20, 19.5, true);
        service.updateServer("survival-2", "survival", 15, 20.0, true);
        service.updateServer("practice-1", "practice", 10, 19.0, true);

        List<ServerSelectorService.ServerInfo> survival = service.getServersForRole("survival");
        assertEquals(2, survival.size());

        List<ServerSelectorService.ServerInfo> practice = service.getServersForRole("practice");
        assertEquals(1, practice.size());
        assertEquals("practice-1", practice.getFirst().serverId());
    }

    @Test
    void onlyReturnsHealthyServers() {
        service.updateServer("survival-1", "survival", 20, 19.5, true);
        service.updateServer("survival-2", "survival", 15, 20.0, false);

        List<ServerSelectorService.ServerInfo> healthy = service.getServersForRole("survival");
        assertEquals(1, healthy.size());
        assertEquals("survival-1", healthy.getFirst().serverId());
    }

    @Test
    void emptyRegistryReturnsEmpty() {
        List<ServerSelectorService.ServerInfo> servers = service.getServersForRole("survival");
        assertTrue(servers.isEmpty());
    }

    @Test
    void removeServerWorks() {
        service.updateServer("survival-1", "survival", 20, 19.5, true);
        service.removeServer("survival-1");

        assertTrue(service.getServersForRole("survival").isEmpty());
    }

    @Test
    void getAllHealthyServersFiltersUnhealthy() {
        service.updateServer("s1", "survival", 10, 20.0, true);
        service.updateServer("s2", "practice", 5, 18.0, false);
        service.updateServer("s3", "creative", 3, 19.0, true);

        assertEquals(2, service.getAllHealthyServers().size());
    }
}

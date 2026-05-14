package net.mythicpvp.core.announce;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link BroadcastService} rotation logic and config loading.
 *
 * <p>The cross-shard echo skip is structurally guarded by the {@code
 * origin} field on {@link BroadcastNotice} and the {@code !equals(shardId)}
 * predicate in {@link BroadcastService#receive}. Pinning that here would
 * require a multi-server harness — the smaller {@link BroadcastNoticeTest}
 * pins the wire shape instead.
 */
class BroadcastServiceTest {

    private ServerMock server;
    private MockPlugin plugin;
    private final ProtocolManager protocolManager = ProtocolManager.getInstance();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        protocolManager.clear();
        MockBukkit.unmock();
    }

    @Test
    void tickAnnouncementReturnsNullWhenDisabled() {
        BroadcastService service = new BroadcastService(protocolManager, "hub-1");
        service.load(configWith("""
            announcements:
              enabled: false
              interval-seconds: 60
              messages:
                - "first"
                - "second"
            """));
        assertNull(service.tickAnnouncement(), "disabled service must not rotate");
    }

    @Test
    void tickAnnouncementReturnsNullWhenNoMessages() {
        BroadcastService service = new BroadcastService(protocolManager, "hub-1");
        service.load(configWith("""
            announcements:
              enabled: true
              interval-seconds: 60
              messages: []
            """));
        assertNull(service.tickAnnouncement());
    }

    @Test
    void tickAnnouncementRotatesAndWraps() {
        BroadcastService service = new BroadcastService(protocolManager, "hub-1");
        service.load(configWith("""
            announcements:
              enabled: true
              interval-seconds: 60
              messages:
                - "alpha"
                - "beta"
                - "gamma"
            """));
        // Need an online player so the broadcast render loop has someone
        // to push to under MockBukkit (otherwise the path is fine but
        // there's nothing to assert).
        server.addPlayer("watcher");

        assertEquals("alpha", service.tickAnnouncement());
        assertEquals("beta", service.tickAnnouncement());
        assertEquals("gamma", service.tickAnnouncement());
        assertEquals("alpha", service.tickAnnouncement(), "rotation wraps to start");
    }

    @Test
    void intervalSecondsHasMinimumFiveSeconds() {
        // Defensive minimum so a typo of `interval-seconds: 0` doesn't
        // turn into a tight loop pegging the scheduler.
        BroadcastService service = new BroadcastService(protocolManager, "hub-1");
        service.load(configWith("""
            announcements:
              enabled: true
              interval-seconds: 1
              messages:
                - "x"
            """));
        assertEquals(5, service.intervalSeconds());
    }

    @Test
    void announcementCountReflectsLoadedMessages() {
        BroadcastService service = new BroadcastService(protocolManager, "hub-1");
        service.load(configWith("""
            announcements:
              enabled: true
              interval-seconds: 60
              messages:
                - "one"
                - "two"
            """));
        assertEquals(2, service.announcementCount());
    }

    @Test
    void enabledFlagSurfacedFromConfig() {
        BroadcastService service = new BroadcastService(protocolManager, "hub-1");
        service.load(configWith("""
            announcements:
              enabled: true
              interval-seconds: 60
              messages: []
            """));
        assertTrue(service.enabled());
    }

    /**
     * Build a {@link MythicConfig} from inline YAML by writing it to a
     * temp file the plugin owns, then loading via the production code
     * path. Avoids fragile reflection / sub-classing.
     */
    private MythicConfig configWith(String yaml) {
        try {
            // MythicConfig resolves files inside the plugin's data folder
            // — point that at our temp dir for the duration of the test.
            File dataFolder = plugin.getDataFolder();
            dataFolder.mkdirs();
            File file = new File(dataFolder, "announcements.yml");
            Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
            return new MythicConfig(plugin, "announcements.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package net.mythicpvp.core.announce;

import org.mockbukkit.mockbukkit.MockBukkit;
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

class BroadcastServiceTest {

    private final ProtocolManager protocolManager = ProtocolManager.getInstance();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
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
        assertEquals("alpha", service.tickAnnouncement());
        assertEquals("beta", service.tickAnnouncement());
        assertEquals("gamma", service.tickAnnouncement());
        assertEquals("alpha", service.tickAnnouncement(), "rotation wraps to start");
    }

    @Test
    void intervalSecondsHasMinimumFiveSeconds() {

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

    private MythicConfig configWith(String yaml) {
        try {

            File dataFolder = tempDir.toFile();
            dataFolder.mkdirs();
            File file = new File(dataFolder, "announcements.yml");
            Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
            return new MythicConfig(dataFolder, "announcements.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

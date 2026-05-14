package net.mythicpvp.core.staffmode;

import net.mythicpvp.suite.config.MythicConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffModeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void toolsLoadFromYaml() {
        StaffModeService service = new StaffModeService();
        service.load(configWith("""
            staff-mode:
              vanish: true
              fly: true
              tools:
                - { slot: 0, material: "BOOK",  name: "&aInspect", type: "INSPECT" }
                - { slot: 8, material: "BARRIER", name: "&cExit",  type: "DISABLE" }
            """));
        assertEquals(2, service.tools().size());
        assertEquals("INSPECT", service.tools().get(0).type());
        assertEquals("DISABLE", service.tools().get(1).type());
    }

    @Test
    void toolsWithUnknownMaterialAreSkipped() {
        StaffModeService service = new StaffModeService();
        service.load(configWith("""
            staff-mode:
              tools:
                - { slot: 0, material: "NOT_A_REAL_MATERIAL", name: "Bad", type: "X" }
                - { slot: 1, material: "STICK", name: "Good", type: "Y" }
            """));
        assertEquals(1, service.tools().size(), "unknown material drops the entry");
        assertEquals("Y", service.tools().get(0).type());
    }

    @Test
    void freezeToggleFlipsFlag() {
        StaffModeService service = new StaffModeService();
        UUID target = UUID.randomUUID();

        assertFalse(service.isFrozen(target));
        assertTrue(service.toggleFreeze(target), "first toggle returns true (now frozen)");
        assertTrue(service.isFrozen(target));
        assertFalse(service.toggleFreeze(target), "second toggle returns false (now thawed)");
        assertFalse(service.isFrozen(target));
    }

    @Test
    void vanishAndFlyFlagsLoad() {
        StaffModeService service = new StaffModeService();
        service.load(configWith("""
            staff-mode:
              vanish: false
              fly: true
              tools: []
            """));
        assertFalse(service.vanishEnabled());
        assertTrue(service.flyEnabled());
    }

    private MythicConfig configWith(String yaml) {
        try {
            File dataFolder = tempDir.toFile();
            dataFolder.mkdirs();
            File file = new File(dataFolder, "staff-mode.yml");
            Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
            return new MythicConfig(dataFolder, "staff-mode.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

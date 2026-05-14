package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.config.MythicConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankCosmeticBundlesTest {

    @TempDir
    Path tempDir;

    @Test
    void emptyConfigYieldsEmptyRegistry() {
        RankCosmeticBundles bundles = new RankCosmeticBundles();
        bundles.load(configWith("ranks: {}"));
        assertEquals(0, bundles.rankIds().size());
        assertTrue(bundles.bundledFor("vip").isEmpty(), "missing rank id returns empty list");
    }

    @Test
    void loadsBundledCosmeticsPerRank() {
        RankCosmeticBundles bundles = new RankCosmeticBundles();
        bundles.load(configWith("""
            ranks:
              vip:
                bundled-cosmetics:
                  - "hat.party_crown"
                  - "title.vip"
              owner:
                bundled-cosmetics:
                  - "title.owner"
              default:
                # no bundled-cosmetics key → not in registry
                permissions: ["mythic.join"]
            """));
        assertEquals(List.of("hat.party_crown", "title.vip"), bundles.bundledFor("vip"));
        assertEquals(List.of("title.owner"), bundles.bundledFor("owner"));
        assertTrue(bundles.bundledFor("default").isEmpty());
        assertEquals(2, bundles.rankIds().size(), "ranks without bundles aren't tracked");
    }

    @Test
    void lookupsAreCaseInsensitive() {
        RankCosmeticBundles bundles = new RankCosmeticBundles();
        bundles.load(configWith("""
            ranks:
              VIP:
                bundled-cosmetics:
                  - "hat.party_crown"
            """));
        assertEquals(List.of("hat.party_crown"), bundles.bundledFor("vip"));
        assertEquals(List.of("hat.party_crown"), bundles.bundledFor("VIP"));
        assertEquals(List.of("hat.party_crown"), bundles.bundledFor("ViP"));
    }

    @Test
    void reloadOverwritesPreviousState() {
        RankCosmeticBundles bundles = new RankCosmeticBundles();
        bundles.load(configWith("""
            ranks:
              vip:
                bundled-cosmetics:
                  - "old.cosmetic"
            """));
        bundles.load(configWith("""
            ranks:
              vip:
                bundled-cosmetics:
                  - "new.cosmetic"
              owner:
                bundled-cosmetics:
                  - "title.owner"
            """));
        assertEquals(List.of("new.cosmetic"), bundles.bundledFor("vip"));
        assertEquals(List.of("title.owner"), bundles.bundledFor("owner"));
    }

    private MythicConfig configWith(String yaml) {
        try {
            File dataFolder = tempDir.toFile();
            dataFolder.mkdirs();
            File file = new File(dataFolder, "ranks.yml");
            Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
            return new MythicConfig(dataFolder, "ranks.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

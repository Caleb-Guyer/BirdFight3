package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdStatsTest {

    @AfterEach
    void restoreDefaults() {
        BirdStats.resetToDefaults();
    }

    @Test
    void appliesOverridesAndIgnoresMalformedValues() {
        Properties props = new Properties();
        props.setProperty("pigeon.speed", "9.9");
        props.setProperty("eagle.power", "12");
        props.setProperty("eagle.unknownStat", "3");
        props.setProperty("pigeon.jumpHeight", "not-a-number");

        int applied = BirdStats.apply(props);

        assertEquals(2, applied);
        assertEquals(9.9, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(12, BirdGame3.BirdType.EAGLE.power);
        assertEquals(BirdGame3.BirdType.PIGEON.defaultJumpHeight, BirdGame3.BirdType.PIGEON.jumpHeight,
                "Malformed values must leave the stat untouched.");
    }

    @Test
    void resetRestoresCompiledDefaults() {
        BirdGame3.BirdType.PIGEON.speed = 42.0;
        BirdGame3.BirdType.GOOSE.power = 99;

        BirdStats.resetToDefaults();

        assertEquals(BirdGame3.BirdType.PIGEON.defaultSpeed, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(BirdGame3.BirdType.GOOSE.defaultPower, BirdGame3.BirdType.GOOSE.power);
    }

    @Test
    void templateRoundTripsThroughReload(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bird-stats.properties");
        assertTrue(BirdStats.writeTemplate(file));
        assertTrue(Files.exists(file));

        String content = Files.readString(file);
        String edited = content.replace(
                "pigeon.speed=" + BirdGame3.BirdType.PIGEON.defaultSpeed,
                "pigeon.speed=7.7");
        Files.writeString(file, edited);

        String summary = BirdStats.reload(file);

        assertTrue(summary != null && summary.contains("OVERRIDES"),
                "Reload should report applied overrides.");
        assertEquals(7.7, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(BirdGame3.BirdType.EAGLE.defaultSpeed, BirdGame3.BirdType.EAGLE.speed,
                "Unedited template lines still apply, at default values.");
    }

    @Test
    void reloadWithoutFileMeansCompiledDefaults(@TempDir Path dir) {
        BirdGame3.BirdType.PIGEON.speed = 42.0;

        String summary = BirdStats.reload(dir.resolve("missing.properties"));

        assertNull(summary);
        assertEquals(BirdGame3.BirdType.PIGEON.defaultSpeed, BirdGame3.BirdType.PIGEON.speed,
                "Reload with no file must reset to compiled defaults.");
    }
}

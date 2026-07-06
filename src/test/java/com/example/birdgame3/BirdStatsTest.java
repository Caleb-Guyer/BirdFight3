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
        BirdGame3.BirdType.EAGLE.damageDealtMult = 2.0;
        BirdGame3.GRAVITY = 3.0;
        Bird.STARTING_HEALTH = 55.0;

        BirdStats.resetToDefaults();

        assertEquals(BirdGame3.BirdType.PIGEON.defaultSpeed, BirdGame3.BirdType.PIGEON.speed);
        assertEquals(BirdGame3.BirdType.GOOSE.defaultPower, BirdGame3.BirdType.GOOSE.power);
        assertEquals(1.0, BirdGame3.BirdType.EAGLE.damageDealtMult);
        assertEquals(BirdGame3.DEFAULT_GRAVITY, BirdGame3.GRAVITY);
        assertEquals(Bird.DEFAULT_STARTING_HEALTH, Bird.STARTING_HEALTH);
    }

    @Test
    void appliesCombatMultipliersAndGlobals() {
        Properties props = new Properties();
        props.setProperty("eagle.damageDealtMult", "1.3");
        props.setProperty("eagle.damageTakenMult", "0.85");
        props.setProperty("pigeon.cooldownRate", "1.5");
        props.setProperty("goose.ultimateRate", "0.75");
        props.setProperty("global.gravity", "0.9");
        props.setProperty("global.startingHealth", "250");

        int applied = BirdStats.apply(props);

        assertEquals(6, applied);
        assertEquals(1.3, BirdGame3.BirdType.EAGLE.damageDealtMult);
        assertEquals(0.85, BirdGame3.BirdType.EAGLE.damageTakenMult);
        assertEquals(1.5, BirdGame3.BirdType.PIGEON.cooldownRate);
        assertEquals(0.75, BirdGame3.BirdType.GOOSE.ultimateRate);
        assertEquals(0.9, BirdGame3.GRAVITY);
        assertEquals(250.0, Bird.STARTING_HEALTH);
    }

    @Test
    void clampsMultipliersAndGlobalsToSaneBands() {
        Properties props = new Properties();
        props.setProperty("eagle.damageDealtMult", "999");
        props.setProperty("pigeon.cooldownRate", "0");
        props.setProperty("global.gravity", "-5");
        props.setProperty("global.startingHealth", "1");

        BirdStats.apply(props);

        assertEquals(10.0, BirdGame3.BirdType.EAGLE.damageDealtMult, "Multiplier should clamp to 10.");
        assertEquals(0.1, BirdGame3.BirdType.PIGEON.cooldownRate, "Multiplier should clamp to 0.1.");
        assertEquals(0.05, BirdGame3.GRAVITY, "Gravity should clamp to its floor.");
        assertEquals(10.0, Bird.STARTING_HEALTH, "Starting health should clamp to its floor.");
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

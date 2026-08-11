package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdGame3ProfileProgressStateTest {
    private Preferences prefs;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("/birdfight3-profile-state-tests/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        prefs.removeNode();
        prefs.flush();
    }

    @Test
    void savesAndLoadsTrainingAcademyDrillBadges() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length,
                4,
                16,
                0,
                0
        );
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.trainingAcademyDrillCompleted[BirdGame3.BirdType.TITMOUSE.ordinal()] = true;
        state.trainingAcademyDrillCompleted[BirdGame3.BirdType.HEISENBIRD.ordinal()] = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.trainingAcademyDrillCompleted[BirdGame3.BirdType.TITMOUSE.ordinal()]);
        assertTrue(loaded.trainingAcademyDrillCompleted[BirdGame3.BirdType.HEISENBIRD.ordinal()]);
        assertFalse(loaded.trainingAcademyDrillCompleted[BirdGame3.BirdType.OPIUMBIRD.ordinal()]);
    }

    @Test
    void savesAndLoadsCrownlockPrisonUnlock() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length,
                4,
                16,
                0,
                0
        );
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.prisonMapUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.prisonMapUnlocked);
    }

    @Test
    void savesAndLoadsRooftopRelayClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length,
                4,
                16,
                0,
                0
        );
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.rooftopRelayUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.rooftopRelayUnlocked);
    }

    @Test
    void savesAndLoadsTempestSummitClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.tempestSummitUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.tempestSummitUnlocked);
    }

    @Test
    void savesAndLoadsPeregrineRunClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.peregrineRunUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.peregrineRunUnlocked);
    }

    @Test
    void savesAndLoadsFrozenCalderaClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.frozenCalderaUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.frozenCalderaUnlocked);
    }

    @Test
    void savesAndLoadsHeartbloomSanctuaryClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.heartbloomSanctuaryUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.heartbloomSanctuaryUnlocked);
    }

    @Test
    void existingPigeonClassicClearMigratesToRooftopRelayReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length,
                4,
                16,
                0,
                0
        );
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.PIGEON.ordinal()] = true;
        legacy.rooftopRelayUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("rooftopRelayUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }

    @Test
    void existingEagleClassicClearMigratesToTempestSummitReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.EAGLE.ordinal()] = true;
        legacy.tempestSummitUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("tempestSummitUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }

    @Test
    void existingFalconClassicClearMigratesToPeregrineRunReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.FALCON.ordinal()] = true;
        legacy.peregrineRunUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("peregrineRunUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }

    @Test
    void existingPhoenixClassicClearMigratesToFrozenCalderaReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.PHOENIX.ordinal()] = true;
        legacy.frozenCalderaUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("frozenCalderaUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }

    @Test
    void existingHummingbirdClassicClearMigratesToHeartbloomReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.HUMMINGBIRD.ordinal()] = true;
        legacy.heartbloomSanctuaryUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("heartbloomSanctuaryUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }
}

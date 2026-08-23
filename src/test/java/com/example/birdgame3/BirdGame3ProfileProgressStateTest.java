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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void blankProfileOffersFirstLaunchTutorialWithoutChangingUnlocks() {
        BirdGame3ProfileProgressState state = BirdGame3ProfileProgressState.load(prefs, schema());

        assertFalse(state.guidedTutorialPromptResolved);
        assertFalse(state.guidedTutorialCompleted);
        assertArrayEquals(new boolean[BirdGame3ProfileProgressState.GUIDED_TUTORIAL_LESSON_COUNT],
                state.guidedTutorialLessonCompleted);
        assertTrue(state.cityPigeonUnlocked);
        assertTrue(state.eagleSkinUnlocked);
        assertFalse(state.falconUnlocked);
    }

    @Test
    void establishedLegacyProfileDoesNotReceiveFirstLaunchInterruption() {
        prefs.putInt("city_wins_0", 1);

        BirdGame3ProfileProgressState state = BirdGame3ProfileProgressState.load(prefs, schema());

        assertTrue(state.guidedTutorialPromptResolved);
        assertFalse(state.guidedTutorialCompleted);
    }

    @Test
    void legacyTutorialCompletionMigratesAllEightLessons() {
        prefs.putBoolean("academy_guided_tutorial_completed", true);

        BirdGame3ProfileProgressState state = BirdGame3ProfileProgressState.load(prefs, schema());

        assertTrue(state.guidedTutorialCompleted);
        for (boolean lesson : state.guidedTutorialLessonCompleted) {
            assertTrue(lesson);
        }
    }

    @Test
    void partialTutorialProgressAndPromptChoiceRoundTripIndependently() {
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.guidedTutorialPromptResolved = true;
        state.guidedTutorialLessonCompleted[0] = true;
        state.guidedTutorialLessonCompleted[3] = true;
        state.trainingAcademyDrillCompleted[BirdGame3.BirdType.RAVEN.ordinal()] = true;

        state.saveTo(prefs, schema());
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema());

        assertTrue(loaded.guidedTutorialPromptResolved);
        assertFalse(loaded.guidedTutorialCompleted);
        assertArrayEquals(state.guidedTutorialLessonCompleted, loaded.guidedTutorialLessonCompleted);
        assertTrue(loaded.trainingAcademyDrillCompleted[BirdGame3.BirdType.RAVEN.ordinal()]);
    }

    @Test
    void savesAndLoadsPerBirdArenaTimeWithoutWallClockData() {
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.typePlayFrames[BirdGame3.BirdType.PIGEON.ordinal()] = 54_321L;
        state.typePlayFrames[BirdGame3.BirdType.RAVEN.ordinal()] = 98_765L;

        state.saveTo(prefs, schema());
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema());

        assertEquals(54_321L, loaded.typePlayFrames[BirdGame3.BirdType.PIGEON.ordinal()]);
        assertEquals(98_765L, loaded.typePlayFrames[BirdGame3.BirdType.RAVEN.ordinal()]);
        assertEquals(0L, loaded.typePlayFrames[BirdGame3.BirdType.GOOSE.ordinal()]);
    }

    private static BirdGame3ProfileProgressState.Schema schema() {
        return new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length,
                4,
                16,
                0,
                0
        );
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
    void savesAndLoadsHarvestTribunalClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.harvestTribunalUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.harvestTribunalUnlocked);
    }

    @Test
    void savesAndLoadsDawnwatchBastionClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.dawnwatchBastionUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.dawnwatchBastionUnlocked);
    }

    @Test
    void savesAndLoadsRedlineCanyonClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.redlineCanyonUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.redlineCanyonUnlocked);
    }

    @Test
    void savesAndLoadsLastIceShelfClassicReward() {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState state = new BirdGame3ProfileProgressState();
        state.lastIceShelfUnlocked = true;

        state.saveTo(prefs, schema);
        BirdGame3ProfileProgressState loaded = BirdGame3ProfileProgressState.load(prefs, schema);

        assertTrue(loaded.lastIceShelfUnlocked);
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

    @Test
    void existingRoosterClassicClearMigratesToDawnwatchBastionReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.ROOSTER.ordinal()] = true;
        legacy.dawnwatchBastionUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("dawnwatchBastionUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }

    @Test
    void existingRoadrunnerClassicClearMigratesToRedlineCanyonReward() throws Exception {
        BirdGame3ProfileProgressState.Schema schema = new BirdGame3ProfileProgressState.Schema(
                BirdGame3Achievement.values().length, 4, 16, 0, 0);
        BirdGame3ProfileProgressState legacy = BirdGame3ProfileProgressState.load(null, schema);
        legacy.classicCompleted[BirdGame3.BirdType.ROADRUNNER.ordinal()] = true;
        legacy.redlineCanyonUnlocked = false;
        BirdGame3 game = new BirdGame3();
        Method apply = BirdGame3.class.getDeclaredMethod(
                "applyProfileProgressState", BirdGame3ProfileProgressState.class);
        apply.setAccessible(true);

        apply.invoke(game, legacy);

        Field unlocked = BirdGame3.class.getDeclaredField("redlineCanyonUnlocked");
        unlocked.setAccessible(true);
        assertTrue(unlocked.getBoolean(game));
    }
}

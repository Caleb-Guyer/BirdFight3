package com.example.birdgame3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
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
}

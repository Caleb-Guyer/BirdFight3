package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialTrailerStoryboardTest {
    @Test
    void sacrificeAndNullRocBeatsUseTheAuthoredCutsceneRenderer() {
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(1));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(2));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(5));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(6));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(8));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(10));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(6));
    }

    @Test
    void storyOpenPromiseAndFinalCardUsePurposeBuiltGraphicShots() {
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(0));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(3));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(11));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(0));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(0));
    }

    @Test
    void fightingCrescendoUsesRealGameplay() {
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(4));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(7));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(9));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(9));
    }

    @Test
    void rosterHeaderClearsTheFirstCharacterRow() {
        assertTrue(BirdGame3.officialTrailerRosterHeaderClearance() >= 40.0);
    }
}

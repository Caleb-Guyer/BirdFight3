package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialTrailerStoryboardTest {
    @Test
    void sacrificeAndNullRocBeatsUseTheAuthoredCutsceneRenderer() {
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(1));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(2));
        assertTrue(BirdGame3.isOfficialTrailerCutsceneScene(12));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(12));
    }

    @Test
    void storyOpenPromiseAndFinalCardUsePurposeBuiltGraphicShots() {
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(0));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(3));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(5));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(7));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(10));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(14));
        assertTrue(BirdGame3.isOfficialTrailerGraphicScene(16));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(0));
        assertFalse(BirdGame3.isOfficialTrailerGameplayScene(0));
    }

    @Test
    void fightingCrescendoUsesRealGameplay() {
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(4));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(6));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(8));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(9));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(11));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(13));
        assertTrue(BirdGame3.isOfficialTrailerGameplayScene(15));
        assertFalse(BirdGame3.isOfficialTrailerCutsceneScene(15));
    }

    @Test
    void everyReleaseTrailerSceneHasExactlyOneRenderer() {
        for (int scene = 0; scene <= 16; scene++) {
            int rendererCount = (BirdGame3.isOfficialTrailerGameplayScene(scene) ? 1 : 0)
                    + (BirdGame3.isOfficialTrailerCutsceneScene(scene) ? 1 : 0)
                    + (BirdGame3.isOfficialTrailerGraphicScene(scene) ? 1 : 0);
            assertEquals(1, rendererCount, "scene " + scene);
        }
    }

    @Test
    void rosterHeaderClearsTheFirstCharacterRow() {
        assertTrue(BirdGame3.officialTrailerRosterHeaderClearance() >= 40.0);
    }
}
